package com.motherhood.journey.identity.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${auth.lockout.max-attempts:5}")
    private int maxAttempts;

    @Value("${auth.lockout.duration-minutes:15}")
    private int lockoutDurationMinutes;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
            .orElseThrow(() -> new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new CustomException("Account is locked. Try again later.", HttpStatus.LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isActive()) {
            throw new CustomException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        user.setLastLoginIp(clientIp());

        return buildResponse(user);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException("Refresh token required", HttpStatus.BAD_REQUEST);
        }
        var rotated = refreshTokenService.rotate(refreshToken, clientIp(), userAgent());
        User user = userRepository.findById(rotated.userId())
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.UNAUTHORIZED));
        String roleStr = user.getRole() != null ? user.getRole().name() : null;
        UUID facilityId = user.getFacility() != null ? user.getFacility().getId() : null;
        String accessToken = jwtUtil.generateToken(user.getPhoneNumber(), roleStr, facilityId);
        return new TokenResponse(accessToken, rotated.rawToken(), roleStr);
    }

    private TokenResponse buildResponse(User user) {
        UUID facilityId = user.getFacility() != null ? user.getFacility().getId() : null;
        String roleStr = user.getRole() != null ? user.getRole().name() : null;
        String accessToken = jwtUtil.generateToken(user.getPhoneNumber(), roleStr, facilityId);
        var refresh = refreshTokenService.issue(user, clientIp(), userAgent());
        return new TokenResponse(accessToken, refresh.rawToken(), roleStr);
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
        }
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getHeader("User-Agent") : null;
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }
}

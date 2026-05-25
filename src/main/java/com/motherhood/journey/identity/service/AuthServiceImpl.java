package com.motherhood.journey.identity.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
            .orElseThrow(() -> new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isActive()) {
            throw new CustomException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        user.setLastLogin(java.time.LocalDateTime.now());

        UUID facilityId = user.getFacility() != null ? user.getFacility().getId() : null;
        String roleStr = user.getRole() != null ? user.getRole().name() : null;
        String token = jwtUtil.generateToken(user.getPhoneNumber(), roleStr, facilityId);

        return new TokenResponse(token, null, roleStr);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        throw new CustomException("Refresh token not supported", HttpStatus.NOT_IMPLEMENTED);
    }
}

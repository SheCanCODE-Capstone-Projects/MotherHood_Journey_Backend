package com.motherhood.identity.application.service;

import com.motherhood.identity.application.dto.LoginRequest;
import com.motherhood.identity.application.dto.TokenResponse;
import com.motherhood.identity.domain.repository.UserRepository;
import com.motherhood.identity.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        var user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Invalid phone number or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid phone number or password");
        }

        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return new TokenResponse(accessToken, refreshToken, user.getRole().name());
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        var userId = jwtTokenProvider.getUserId(refreshToken);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        return new TokenResponse(newAccessToken, refreshToken, user.getRole().name());
    }
}

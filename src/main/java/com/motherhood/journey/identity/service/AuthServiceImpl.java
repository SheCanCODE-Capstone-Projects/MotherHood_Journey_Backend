package com.motherhood.journey.identity.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.identity.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.phone())
                .orElseThrow(() -> new CustomException("Invalid phone number or password", HttpStatus.UNAUTHORIZED));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new CustomException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException("Invalid phone number or password", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String subject = user.getId().toString();
        String role    = user.getRole();

        return new TokenResponse(
                jwtService.generateAccessToken(subject, role),
                jwtService.generateRefreshToken(subject, role),
                role
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        String userId;
        try {
            userId = jwtService.validateAndGetSubject(refreshToken, "refresh");
        } catch (JwtException e) {
            throw new CustomException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.UNAUTHORIZED));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new CustomException("Account is disabled", HttpStatus.FORBIDDEN);
        }

        String role = user.getRole();
        return new TokenResponse(
                jwtService.generateAccessToken(userId, role),
                refreshToken,
                role
        );
    }
}

package com.motherhood.journey.identity.service;

import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
}

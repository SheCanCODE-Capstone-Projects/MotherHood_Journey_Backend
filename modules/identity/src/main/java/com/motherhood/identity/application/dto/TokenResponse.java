package com.motherhood.identity.application.dto;

public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String role;

    public TokenResponse(String accessToken, String refreshToken, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getRole() { return role; }
}

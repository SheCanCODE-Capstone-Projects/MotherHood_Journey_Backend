package com.motherhood.journey.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.dto.request.LoginRequest;
import com.motherhood.journey.identity.dto.request.RefreshRequest;
import com.motherhood.journey.identity.dto.request.RegisterRequest;
import com.motherhood.journey.identity.dto.response.TokenResponse;
import com.motherhood.journey.identity.dto.response.UserResponse;
import com.motherhood.journey.identity.entity.Role;
import com.motherhood.journey.identity.service.AuthService;
import com.motherhood.journey.identity.service.RefreshTokenService;
import com.motherhood.journey.identity.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AuthControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean UserService userService;
    @MockBean RefreshTokenService refreshTokenService;

    @Test
    void login_validBody_returns200WithTokens() throws Exception {
        when(authService.login(any())).thenReturn(new TokenResponse("access", "refresh", "PATIENT"));
        var req = new LoginRequest("+250700000001", "supersecret");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("access"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void login_missingPhoneAndPassword_returns400() throws Exception {
        var req = new LoginRequest("", "");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any()))
            .thenThrow(new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        var req = new LoginRequest("+250700000001", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validToken_returns200() throws Exception {
        when(authService.refresh(anyString()))
            .thenReturn(new TokenResponse("new-access", "new-refresh", "PATIENT"));
        var req = new RefreshRequest("token-123");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").value("new-access"));
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        var req = new RefreshRequest("");
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void logout_revokesAndReturns200() throws Exception {
        var req = new RefreshRequest("token-to-revoke");

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID geo = UUID.randomUUID();
        when(userService.register(any())).thenReturn(new UserResponse(
            id, "+250700000099", "1199912345678901", "Jane", "Doe",
            "PATIENT", "rw", true, null, geo, LocalDateTime.now(), null));

        var req = new RegisterRequest(
            "+250700000099", "1199912345678901", "supersecret",
            "Jane", "Doe", Role.PATIENT, geo, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.phoneNumber").value("+250700000099"));
    }

    @Test
    void register_missingRequiredFields_returns400() throws Exception {
        var req = new RegisterRequest("", "", "short", "", "",
            null, null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}

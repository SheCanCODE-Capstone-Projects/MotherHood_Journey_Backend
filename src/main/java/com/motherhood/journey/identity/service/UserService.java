package com.motherhood.journey.identity.service;

import com.motherhood.journey.identity.dto.request.RegisterRequest;
import com.motherhood.journey.identity.dto.request.UpdateUserRequest;
import com.motherhood.journey.identity.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse register(RegisterRequest request);
    UserResponse getUser(UUID id);
    UserResponse updateUser(UUID id, UpdateUserRequest request);
}

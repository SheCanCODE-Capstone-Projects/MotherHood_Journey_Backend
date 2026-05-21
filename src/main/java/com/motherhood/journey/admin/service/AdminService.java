package com.motherhood.journey.admin.service;

import com.motherhood.journey.admin.dto.response.AdminDashboardResponse;
import com.motherhood.journey.identity.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminService {
    AdminDashboardResponse getDashboard();
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserResponse deactivateUser(UUID userId);
    UserResponse activateUser(UUID userId);
}

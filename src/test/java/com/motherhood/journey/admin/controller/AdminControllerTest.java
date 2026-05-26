package com.motherhood.journey.admin.controller;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.admin.dto.response.AdminDashboardResponse;
import com.motherhood.journey.admin.service.AdminService;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AdminControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;

    @MockBean AdminService adminService;

    @Test
    void dashboard_asMohAdmin_returns200() throws Exception {
        when(adminService.getDashboard()).thenReturn(
            new AdminDashboardResponse(1, 10, 5, 20, 3, 4, 2));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void dashboard_unauthenticated_returns401or403() throws Exception {
        int sc = mockMvc.perform(get("/api/v1/admin/dashboard"))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void dashboard_asPatient_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .with(user("p").roles("PATIENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboard_asFacilityAdmin_returns403() throws Exception {
        // Only MOH_ADMIN allowed
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .with(user("fa").roles("FACILITY_ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_asMohAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<UserResponse> page = new PageImpl<>(List.of(new UserResponse(
            id, "+250700000001", "1199912345678901", "Jane", "Doe",
            "PATIENT", "rw", true, null, UUID.randomUUID(),
            LocalDateTime.now(), null)));
        when(adminService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void deactivateUser_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(adminService.deactivateUser(eq(id)))
            .thenThrow(new CustomException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(patch("/api/v1/admin/users/" + id + "/deactivate")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isNotFound());
    }
}

package com.motherhood.journey.maternal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.maternal.dto.request.CreatedMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.dto.response.MotherSummaryResponse;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import com.motherhood.journey.maternal.service.MotherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class MotherControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean MotherService motherService;

    private MotherResponse sampleMother(UUID id) {
        return new MotherResponse(
            id, UUID.randomUUID(), "MH-2025-000001", "VERIFIED",
            LocalDate.of(1990, 1, 1), null,
            UUID.randomUUID(), "Test Facility",
            UUID.randomUUID(), "Sector", "Cell", "Village",
            LocalDateTime.now());
    }

    @Test
    void register_asHealthWorker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(motherService.registerMother(any())).thenReturn(sampleMother(id));

        var req = new CreatedMotherRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "1199912345678901", UUID.randomUUID(),
            LocalDate.of(1990, 1, 1), null);

        mockMvc.perform(post("/api/v1/mothers")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void register_unauthenticated_returns401or403() throws Exception {
        var req = new CreatedMotherRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "1199912345678901", UUID.randomUUID(),
            LocalDate.of(1990, 1, 1), null);

        int sc = mockMvc.perform(post("/api/v1/mothers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void register_wrongRole_returns403() throws Exception {
        var req = new CreatedMotherRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "1199912345678901", UUID.randomUUID(),
            LocalDate.of(1990, 1, 1), null);

        mockMvc.perform(post("/api/v1/mothers")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void register_invalidNationalId_returns400() throws Exception {
        var req = new CreatedMotherRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "INVALID", UUID.randomUUID(),
            LocalDate.of(1990, 1, 1), null);

        mockMvc.perform(post("/api/v1/mothers")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getByHealthId_asHealthWorker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(motherService.getByHealthId(eq("MH-2025-000001"))).thenReturn(sampleMother(id));

        mockMvc.perform(get("/api/v1/mothers/health/MH-2025-000001")
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isOk());
    }

    @Test
    void getMotherById_asHealthWorkerWithAppUser_returns200() throws Exception {
        UUID motherId = UUID.randomUUID();
        when(motherService.getMotherById(eq(motherId), any())).thenReturn(sampleMother(motherId));

        User appUser = User.builder()
            .id(UUID.randomUUID())
            .phoneNumber("+250700000001")
            .nationalId("1199912345678901")
            .passwordHash("hash")
            .firstName("Jane").lastName("Doe")
            .role(UserRole.HEALTH_WORKER)
            .preferredLanguage("rw")
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

        var auth = new UsernamePasswordAuthenticationToken(
            appUser, null,
            List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));

        mockMvc.perform(get("/api/v1/mothers/" + motherId)
                .with(authentication(auth)))
            .andExpect(status().isOk());
    }

    @Test
    void getPendingNida_asMohAdmin_returns403_dueToUrlGate() throws Exception {
        // /api/v1/mothers/** is restricted at the URL level to HEALTH_WORKER, FACILITY_ADMIN
        // so MOH_ADMIN is rejected by the URL filter even though @PreAuthorize allows it.
        when(motherService.getPendingNidaVerification()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/mothers/pending-nida")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void getPendingNida_asFacilityAdmin_returns200() throws Exception {
        when(motherService.getPendingNidaVerification()).thenReturn(List.of(
            new MotherSummaryResponse(UUID.randomUUID(), "MH-2025-000001",
                NidaVerifiedStatus.PENDING, LocalDate.of(1990, 1, 1), LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/mothers/pending-nida")
                .with(user("fa").roles("FACILITY_ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void getByHealthId_notFound_returns404_whenServiceThrowsCustomException() throws Exception {
        when(motherService.getByHealthId(eq("MH-9999-999999")))
            .thenThrow(new CustomException("Mother not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/mothers/health/MH-9999-999999")
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isNotFound());
    }
}

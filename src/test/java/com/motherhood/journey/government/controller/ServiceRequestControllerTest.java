package com.motherhood.journey.government.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.dto.request.SubmitServiceRequestRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestResponse;
import com.motherhood.journey.government.enums.ServiceRequestStatus;
import com.motherhood.journey.government.enums.ServiceType;
import com.motherhood.journey.government.service.ServiceRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ServiceRequestControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ServiceRequestService serviceRequestService;

    private ServiceRequestResponse sample(UUID id) {
        return new ServiceRequestResponse(
            id, "SR-001", "BIRTH_CERT",
            ServiceRequestStatus.PENDING,
            UUID.randomUUID(), UUID.randomUUID(),
            null, LocalDateTime.now(), null);
    }

    @Test
    void submit_anyAuthenticatedUser_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRequestService.submit(any())).thenReturn(sample(id));

        var req = new SubmitServiceRequestRequest(
            ServiceType.BIRTH_CERT,
            UUID.randomUUID(), UUID.randomUUID(),
            Map.of("k", "v"));

        mockMvc.perform(post("/api/v1/service-requests")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void submit_unauthenticated_returns401or403() throws Exception {
        var req = new SubmitServiceRequestRequest(
            ServiceType.BIRTH_CERT,
            UUID.randomUUID(), UUID.randomUUID(), Map.of());

        int sc = mockMvc.perform(post("/api/v1/service-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void submit_missingFacilityId_returns400() throws Exception {
        var req = new SubmitServiceRequestRequest(
            ServiceType.BIRTH_CERT,
            null, UUID.randomUUID(), Map.of());

        mockMvc.perform(post("/api/v1/service-requests")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getById_asMohAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRequestService.getById(eq(id))).thenReturn(sample(id));

        mockMvc.perform(get("/api/v1/service-requests/" + id)
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void getById_asPatient_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/service-requests/" + id)
                .with(user("p").roles("PATIENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRequestService.getById(eq(id)))
            .thenThrow(new CustomException("Not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/service-requests/" + id)
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void approve_asFacilityAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRequestService.approve(eq(id))).thenReturn(sample(id));

        mockMvc.perform(patch("/api/v1/service-requests/" + id + "/approve")
                .with(user("fa").roles("FACILITY_ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void approve_alreadyResolved_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRequestService.approve(eq(id)))
            .thenThrow(new CustomException("Already resolved", HttpStatus.CONFLICT));

        mockMvc.perform(patch("/api/v1/service-requests/" + id + "/approve")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isConflict());
    }

    @Test
    void reject_asMohAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRequestService.reject(eq(id), eq("missing docs"))).thenReturn(sample(id));

        mockMvc.perform(patch("/api/v1/service-requests/" + id + "/reject")
                .param("reason", "missing docs")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isOk());
    }
}

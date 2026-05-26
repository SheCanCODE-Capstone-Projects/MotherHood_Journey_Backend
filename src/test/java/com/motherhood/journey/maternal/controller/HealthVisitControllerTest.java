package com.motherhood.journey.maternal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;
import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.maternal.enums.VisitType;
import com.motherhood.journey.maternal.service.HealthVisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class HealthVisitControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean HealthVisitService healthVisitService;

    private HealthVisitResponse sample(UUID id) {
        return new HealthVisitResponse(
            id, UUID.randomUUID(), "MOTHER", UUID.randomUUID(),
            UUID.randomUUID(), LocalDateTime.now().minusHours(1),
            "ANC", "headache", 65.0, 165.0, 120, 80, 24.0,
            "stable", LocalDateTime.now(), List.of(), List.of());
    }

    @Test
    void createVisit_asHealthWorker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(healthVisitService.createVisit(any())).thenReturn(sample(id));

        var req = new CreateHealthVisitRequest(
            UUID.randomUUID(), PatientType.MOTHER,
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            LocalDateTime.now().minusHours(1), VisitType.ANC,
            "headache", 65.0, 165.0, 120, 80, 24.0, "stable",
            null, null);

        mockMvc.perform(post("/api/v1/health-visits")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void createVisit_unauthenticated_returns401or403() throws Exception {
        var req = new CreateHealthVisitRequest(
            UUID.randomUUID(), PatientType.MOTHER,
            UUID.randomUUID(), UUID.randomUUID(), null,
            LocalDateTime.now().minusHours(1), VisitType.ANC,
            null, null, null, null, null, null, null, null, null);

        int sc = mockMvc.perform(post("/api/v1/health-visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void createVisit_asPatient_returns403() throws Exception {
        var req = new CreateHealthVisitRequest(
            UUID.randomUUID(), PatientType.MOTHER,
            UUID.randomUUID(), UUID.randomUUID(), null,
            LocalDateTime.now().minusHours(1), VisitType.ANC,
            null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/health-visits")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createVisit_invalidWeight_returns400() throws Exception {
        var req = new CreateHealthVisitRequest(
            UUID.randomUUID(), PatientType.MOTHER,
            UUID.randomUUID(), UUID.randomUUID(), null,
            LocalDateTime.now().minusHours(1), VisitType.ANC,
            null, 500.0, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/health-visits")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getVisitsByFacility_asDistrictOfficer_returns200() throws Exception {
        UUID facilityId = UUID.randomUUID();
        Page<HealthVisitResponse> page = new PageImpl<>(List.of(sample(UUID.randomUUID())));
        when(healthVisitService.getVisitsByFacility(eq(facilityId), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/health-visits/by-facility/" + facilityId)
                .with(user("d").roles("DISTRICT_OFFICER")))
            .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(healthVisitService.getVisitById(eq(id), eq(facilityId)))
            .thenThrow(new CustomException("Visit not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/health-visits/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isNotFound());
    }
}

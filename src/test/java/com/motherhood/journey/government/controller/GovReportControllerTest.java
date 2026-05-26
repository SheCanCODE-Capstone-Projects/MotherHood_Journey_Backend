package com.motherhood.journey.government.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.government.dto.request.GovReportRequest;
import com.motherhood.journey.government.dto.response.GovReportResponse;
import com.motherhood.journey.government.enums.ReportType;
import com.motherhood.journey.government.enums.ScopeLevel;
import com.motherhood.journey.government.service.GovReportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class GovReportControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean GovReportService govReportService;

    private GovReportResponse sample(UUID id) {
        return new GovReportResponse(
            id, UUID.randomUUID(), UUID.randomUUID(),
            "VACCINATION_COVERAGE", "2025-Q1", "NATIONAL",
            Map.of("count", 100), "PENDING",
            LocalDateTime.now(), null);
    }

    @Test
    void generate_asMohAdmin_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(govReportService.generate(any())).thenReturn(sample(id));

        var req = new GovReportRequest(
            ReportType.VACCINATION_COVERAGE, "2025-Q1",
            ScopeLevel.NATIONAL, UUID.randomUUID(), Map.of());

        mockMvc.perform(post("/api/v1/gov-reports")
                .with(user("admin").roles("MOH_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void generate_unauthenticated_returns401or403() throws Exception {
        var req = new GovReportRequest(
            ReportType.VACCINATION_COVERAGE, "2025-Q1",
            ScopeLevel.NATIONAL, UUID.randomUUID(), Map.of());

        int sc = mockMvc.perform(post("/api/v1/gov-reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void generate_asPatient_returns403() throws Exception {
        var req = new GovReportRequest(
            ReportType.VACCINATION_COVERAGE, "2025-Q1",
            ScopeLevel.NATIONAL, UUID.randomUUID(), Map.of());

        mockMvc.perform(post("/api/v1/gov-reports")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void generate_asHealthWorker_returns403() throws Exception {
        var req = new GovReportRequest(
            ReportType.VACCINATION_COVERAGE, "2025-Q1",
            ScopeLevel.NATIONAL, UUID.randomUUID(), Map.of());

        mockMvc.perform(post("/api/v1/gov-reports")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void generate_missingReportType_returns400() throws Exception {
        var req = new GovReportRequest(
            null, "2025-Q1",
            ScopeLevel.NATIONAL, UUID.randomUUID(), Map.of());

        mockMvc.perform(post("/api/v1/gov-reports")
                .with(user("admin").roles("MOH_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getById_asDistrictOfficer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(govReportService.getById(eq(id))).thenReturn(sample(id));

        mockMvc.perform(get("/api/v1/gov-reports/" + id)
                .with(user("d").roles("DISTRICT_OFFICER")))
            .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(govReportService.getById(eq(id)))
            .thenThrow(new CustomException("Report not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/gov-reports/" + id)
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isNotFound());
    }
}

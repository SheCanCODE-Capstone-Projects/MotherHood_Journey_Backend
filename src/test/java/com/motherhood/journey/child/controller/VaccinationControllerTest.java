package com.motherhood.journey.child.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.dto.response.VaccinationRecordResponse;
import com.motherhood.journey.child.service.VaccinationService;
import com.motherhood.journey.common.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class VaccinationControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean VaccinationService vaccinationService;

    private VaccinationRecordResponse sample(UUID id) {
        return new VaccinationRecordResponse(
            id, UUID.randomUUID(), UUID.randomUUID(),
            "BCG", "BCG", UUID.randomUUID(), UUID.randomUUID(),
            LocalDate.now().minusDays(1), LocalDate.now(),
            "LOT-1", "ADMINISTERED", "ok", LocalDateTime.now());
    }

    @Test
    void administer_asHealthWorker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(vaccinationService.administer(eq(id), eq(facilityId), any()))
            .thenReturn(sample(id));

        var req = new AdministerVaccinationRequest(
            UUID.randomUUID(), LocalDate.now(), "LOT-1", "ok");

        mockMvc.perform(patch("/api/v1/vaccinations/" + id + "/administer")
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void administer_unauthenticated_returns401or403() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new AdministerVaccinationRequest(
            UUID.randomUUID(), LocalDate.now(), "LOT-1", "ok");

        int sc = mockMvc.perform(patch("/api/v1/vaccinations/" + id + "/administer")
                .param("facilityId", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void administer_wrongRole_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new AdministerVaccinationRequest(
            UUID.randomUUID(), LocalDate.now(), "LOT-1", "ok");

        mockMvc.perform(patch("/api/v1/vaccinations/" + id + "/administer")
                .param("facilityId", UUID.randomUUID().toString())
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void administer_missingAdministeredById_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        var req = new AdministerVaccinationRequest(
            null, LocalDate.now(), "LOT-1", "ok");

        mockMvc.perform(patch("/api/v1/vaccinations/" + id + "/administer")
                .param("facilityId", UUID.randomUUID().toString())
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void administer_recordNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(vaccinationService.administer(eq(id), eq(facilityId), any()))
            .thenThrow(new CustomException("Vaccination record not found", HttpStatus.NOT_FOUND));

        var req = new AdministerVaccinationRequest(
            UUID.randomUUID(), LocalDate.now(), "LOT-1", "ok");

        mockMvc.perform(patch("/api/v1/vaccinations/" + id + "/administer")
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isNotFound());
    }
}

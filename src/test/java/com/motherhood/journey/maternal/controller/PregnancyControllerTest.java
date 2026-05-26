package com.motherhood.journey.maternal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.maternal.dto.request.CreatePregnancyRequest;
import com.motherhood.journey.maternal.dto.request.UpdatePregnancyRequest;
import com.motherhood.journey.maternal.dto.response.PregnancyResponse;
import com.motherhood.journey.maternal.enums.PregnancyStatus;
import com.motherhood.journey.maternal.service.PregnancyService;
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
import java.util.List;
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
class PregnancyControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PregnancyService pregnancyService;

    private PregnancyResponse sample(UUID id, UUID motherId) {
        return new PregnancyResponse(
            id, motherId,
            LocalDate.now().minusMonths(2), LocalDate.now().plusMonths(7),
            PregnancyStatus.ACTIVE, 1, 0, null, null,
            LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_asHealthWorker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID motherId = UUID.randomUUID();
        when(pregnancyService.createPregnancy(any())).thenReturn(sample(id, motherId));

        var req = new CreatePregnancyRequest(motherId, LocalDate.now().minusMonths(2),
            null, 1, 0, null);

        mockMvc.perform(post("/api/v1/pregnancies")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void create_unauthenticated_returns401or403() throws Exception {
        var req = new CreatePregnancyRequest(UUID.randomUUID(),
            LocalDate.now().minusMonths(2), null, 1, 0, null);
        int sc = mockMvc.perform(post("/api/v1/pregnancies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void create_wrongRole_returns403() throws Exception {
        var req = new CreatePregnancyRequest(UUID.randomUUID(),
            LocalDate.now().minusMonths(2), null, 1, 0, null);

        mockMvc.perform(post("/api/v1/pregnancies")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void create_missingMotherId_returns400() throws Exception {
        var req = new CreatePregnancyRequest(null,
            LocalDate.now().minusMonths(2), null, 1, 0, null);

        mockMvc.perform(post("/api/v1/pregnancies")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_alreadyActivePregnancy_returns409() throws Exception {
        when(pregnancyService.createPregnancy(any()))
            .thenThrow(new CustomException("Mother already has active pregnancy", HttpStatus.CONFLICT));

        var req = new CreatePregnancyRequest(UUID.randomUUID(),
            LocalDate.now().minusMonths(2), null, 1, 0, null);

        mockMvc.perform(post("/api/v1/pregnancies")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    void getById_asHealthWorker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(pregnancyService.getPregnancyById(eq(id), eq(facilityId)))
            .thenReturn(sample(id, UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/pregnancies/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(pregnancyService.getPregnancyById(eq(id), eq(facilityId)))
            .thenThrow(new CustomException("Pregnancy not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/pregnancies/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_asHealthWorker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(pregnancyService.updatePregnancy(eq(id), eq(facilityId), any()))
            .thenReturn(sample(id, UUID.randomUUID()));

        var req = new UpdatePregnancyRequest(null, null, "DELIVERED", null, null, null, "normal birth");

        mockMvc.perform(patch("/api/v1/pregnancies/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void update_illegalTransition_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(pregnancyService.updatePregnancy(eq(id), eq(facilityId), any()))
            .thenThrow(new CustomException("Cannot reopen terminal pregnancy", HttpStatus.CONFLICT));

        var req = new UpdatePregnancyRequest(null, null, "ACTIVE", null, null, null, null);

        mockMvc.perform(patch("/api/v1/pregnancies/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict());
    }

    @Test
    void listByMother_asDistrictOfficer_returns200() throws Exception {
        UUID motherId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(pregnancyService.getPregnanciesByMother(eq(motherId), eq(facilityId)))
            .thenReturn(List.of(sample(UUID.randomUUID(), motherId)));

        mockMvc.perform(get("/api/v1/pregnancies/by-mother/" + motherId)
                .param("facilityId", facilityId.toString())
                .with(user("d").roles("DISTRICT_OFFICER")))
            .andExpect(status().isOk());
    }
}

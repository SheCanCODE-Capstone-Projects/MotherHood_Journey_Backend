package com.motherhood.journey.consent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.consent.dto.request.CreateConsentRequest;
import com.motherhood.journey.consent.dto.response.ConsentResponse;
import com.motherhood.journey.consent.service.ConsentService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ConsentControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ConsentService consentService;

    private ConsentResponse sample(UUID id, UUID motherId) {
        return new ConsentResponse(
            id, motherId, "DATA_SHARING", true,
            "HEALTH_WORKER", LocalDateTime.now(), null,
            "Law 058/2021", null);
    }

    @Test
    void createConsent_asHealthWorker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID motherId = UUID.randomUUID();
        when(consentService.createConsent(any())).thenReturn(sample(id, motherId));

        var req = new CreateConsentRequest(
            motherId, "DATA_SHARING", true,
            "HEALTH_WORKER", null, "Law 058/2021");

        mockMvc.perform(post("/api/v1/consents")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void createConsent_unauthenticated_returns401or403() throws Exception {
        var req = new CreateConsentRequest(
            UUID.randomUUID(), "DATA_SHARING", true,
            null, null, null);

        int sc = mockMvc.perform(post("/api/v1/consents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void createConsent_asPatient_returns403() throws Exception {
        var req = new CreateConsentRequest(
            UUID.randomUUID(), "DATA_SHARING", true,
            null, null, null);

        mockMvc.perform(post("/api/v1/consents")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void createConsent_missingMotherId_returns400() throws Exception {
        var req = new CreateConsentRequest(
            null, "DATA_SHARING", true,
            null, null, null);

        mockMvc.perform(post("/api/v1/consents")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void revokeConsent_asHealthWorker_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/consents/" + id + "/revoke")
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isNoContent());
    }

    @Test
    void revokeConsent_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        doThrow(new CustomException("Consent not found", HttpStatus.NOT_FOUND))
            .when(consentService).revokeConsent(eq(id), eq(facilityId));

        mockMvc.perform(patch("/api/v1/consents/" + id + "/revoke")
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isNotFound());
    }
}

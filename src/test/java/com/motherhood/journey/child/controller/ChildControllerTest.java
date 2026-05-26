package com.motherhood.journey.child.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.enums.DeliveryType;
import com.motherhood.journey.child.enums.Gender;
import com.motherhood.journey.child.service.ChildService;
import com.motherhood.journey.common.exception.CustomException;
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

import java.time.LocalDate;
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
class ChildControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ChildService childService;

    private ChildResponse sample(UUID id, UUID motherId, UUID facilityId) {
        return new ChildResponse(id, motherId, facilityId, UUID.randomUUID(),
            "BC-001", "Baby", "MALE", LocalDate.now().minusDays(10),
            3.2, "NORMAL", "HEALTHY", 0, LocalDateTime.now());
    }

    @Test
    void registerChild_asHealthWorker_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID motherId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(childService.registerChild(any())).thenReturn(sample(id, motherId, facilityId));

        var req = new CreateChildRequest(
            motherId, facilityId, UUID.randomUUID(),
            "BC-001", "Baby", Gender.MALE,
            LocalDate.now().minusDays(10), 3.2, DeliveryType.NORMAL);

        mockMvc.perform(post("/api/v1/children")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void registerChild_unauthenticated_returns401or403() throws Exception {
        var req = new CreateChildRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "BC-001", "Baby", Gender.MALE,
            LocalDate.now().minusDays(10), 3.2, DeliveryType.NORMAL);

        int sc = mockMvc.perform(post("/api/v1/children")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void registerChild_wrongRole_returns403() throws Exception {
        var req = new CreateChildRequest(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "BC-001", "Baby", Gender.MALE,
            LocalDate.now().minusDays(10), 3.2, DeliveryType.NORMAL);

        mockMvc.perform(post("/api/v1/children")
                .with(user("p").roles("PATIENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void registerChild_missingMotherId_returns400() throws Exception {
        var req = new CreateChildRequest(
            null, UUID.randomUUID(), UUID.randomUUID(),
            "BC-001", "Baby", Gender.MALE,
            LocalDate.now().minusDays(10), 3.2, DeliveryType.NORMAL);

        mockMvc.perform(post("/api/v1/children")
                .with(user("hw").roles("HEALTH_WORKER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getById_asHealthWorker_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(childService.getChildById(eq(id), eq(facilityId)))
            .thenReturn(sample(id, UUID.randomUUID(), facilityId));

        mockMvc.perform(get("/api/v1/children/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        when(childService.getChildById(eq(id), eq(facilityId)))
            .thenThrow(new CustomException("Child not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/children/" + id)
                .param("facilityId", facilityId.toString())
                .with(user("hw").roles("HEALTH_WORKER")))
            .andExpect(status().isNotFound());
    }

    @Test
    void listByMother_asFacilityAdmin_returns200() throws Exception {
        UUID motherId = UUID.randomUUID();
        UUID facilityId = UUID.randomUUID();
        Page<ChildResponse> page = new PageImpl<>(
            List.of(sample(UUID.randomUUID(), motherId, facilityId)));
        when(childService.getChildrenByMother(eq(motherId), eq(facilityId), any()))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/children/by-mother/" + motherId)
                .param("facilityId", facilityId.toString())
                .with(user("fa").roles("FACILITY_ADMIN")))
            .andExpect(status().isOk());
    }
}

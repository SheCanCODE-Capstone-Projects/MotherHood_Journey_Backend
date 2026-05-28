package com.motherhood.journey.government.controller;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.enums.TargetSystem;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class GovSyncAdminControllerTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;

    @MockBean GovSyncLogRepository repository;

    private GovSyncLog sampleEntry(UUID id) {
        return GovSyncLog.builder()
            .id(id)
            .targetSystem(TargetSystem.NIDA)
            .syncType("BIRTH_REGISTRATION")
            .status(SyncStatus.FAILED)
            .idempotencyKey("idem-" + id)
            .deadLetter(true)
            .retryCount(5)
            .build();
    }

    @Test
    void retry_asMohAdmin_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findById(eq(id))).thenReturn(Optional.of(sampleEntry(id)));

        mockMvc.perform(post("/api/v1/admin/gov-sync/" + id + "/retry")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void retry_asFacilityAdmin_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/gov-sync/" + id + "/retry")
                .with(user("fa").roles("FACILITY_ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void retry_unauthenticated_returns401or403() throws Exception {
        UUID id = UUID.randomUUID();
        int sc = mockMvc.perform(post("/api/v1/admin/gov-sync/" + id + "/retry"))
            .andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
    }

    @Test
    void retry_asPatient_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/gov-sync/" + id + "/retry")
                .with(user("p").roles("PATIENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    void retry_asDistrictOfficer_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/gov-sync/" + id + "/retry")
                .with(user("d").roles("DISTRICT_OFFICER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void retry_entryNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findById(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/admin/gov-sync/" + id + "/retry")
                .with(user("admin").roles("MOH_ADMIN")))
            .andExpect(status().isNotFound());
    }
}

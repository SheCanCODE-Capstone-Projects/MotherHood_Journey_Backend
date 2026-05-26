package com.motherhood.journey.government.service;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.consent.entity.ConsentRecord;
import com.motherhood.journey.consent.repository.ConsentRepository;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.government.dto.request.SubmitServiceRequestRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestResponse;
import com.motherhood.journey.government.enums.ServiceType;
import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.enums.TargetSystem;
import com.motherhood.journey.government.repository.GovSyncLogRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * E2E: mother submits a government service request.
 * Verifies consent gating, reference-number format (SR-YYYY-NNNNN), and
 * that an outbox GovSyncLog row was created with status=PENDING.
 */
@Transactional
class ServiceRequestFlowIntegrationTest extends IntegrationTestBase {

    @Autowired ServiceRequestService serviceRequestService;
    @Autowired UserRepository userRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired GeoRepository geoRepository;
    @Autowired MotherRepository motherRepository;
    @Autowired ConsentRepository consentRepository;
    @Autowired GovSyncLogRepository govSyncLogRepository;

    private User motherUser;
    private Mother mother;
    private Facility facility;
    private GeoLocation geo;

    @BeforeEach
    void seed() {
        geo = geoRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V19 geo seed missing"));

        facility = facilityRepository.save(Facility.builder()
            .geoLocation(geo)
            .name("Test Health Center")
            .facilityCode("SR-" + UUID.randomUUID().toString().substring(0, 6))
            .facilityType(FacilityType.HEALTH_CENTER)
            .district(geo.getDistrict())
            .build());

        motherUser = userRepository.save(User.builder()
            .nationalId("1" + System.nanoTime())
            .phoneNumber("+250788" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.PATIENT)
            .firstName("Mama").lastName("SR")
            .preferredLanguage("rw")
            .facility(facility)
            .geoLocation(geo)
            .build());

        mother = motherRepository.save(Mother.builder()
            .user(motherUser)
            .facility(facility)
            .geoLocation(geo)
            .healthId("MH-2026-200001")
            .dateOfBirth(LocalDate.of(1995, 5, 1))
            .build());

        var auth = new UsernamePasswordAuthenticationToken(
            motherUser.getPhoneNumber(), null,
            List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        auth.setDetails(new FacilityAuthDetails(facility.getId()));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void submit_withoutConsent_isRejected() {
        SubmitServiceRequestRequest req = new SubmitServiceRequestRequest(
            ServiceType.BIRTH_CERT, facility.getId(), geo.getId(), Map.of("note", "test"));

        assertThatThrownBy(() -> serviceRequestService.submit(req))
            .hasMessageContaining("GOV_DATA_SHARE consent is required");
    }

    @Test
    void submit_withConsent_persistsAndQueuesOutbox() {
        consentRepository.save(ConsentRecord.builder()
            .mother(mother)
            .consentType("GOV_DATA_SHARE")
            .granted(true)
            .grantedByRole("PATIENT")
            .consentedAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusYears(1))
            .legalBasis("LAW_058_2021")
            .build());

        SubmitServiceRequestRequest req = new SubmitServiceRequestRequest(
            ServiceType.BIRTH_CERT, facility.getId(), geo.getId(), Map.of("note", "test"));

        ServiceRequestResponse response = serviceRequestService.submit(req);

        assertThat(response.referenceNo())
            .as("Reference number must match SR-YYYY-NNNNN")
            .matches("^SR-\\d{4}-\\d{5}$");
        assertThat(response.status()).isEqualTo("PENDING");

        // Outbox row exists for async dispatch
        var outboxRows = govSyncLogRepository.findAll().stream()
            .filter(r -> r.getTargetSystem() == TargetSystem.IREMBO
                      && r.getStatus() == SyncStatus.PENDING)
            .toList();
        assertThat(outboxRows)
            .as("Outbox should hold one PENDING IREMBO row for the submitted SR")
            .isNotEmpty();
    }
}

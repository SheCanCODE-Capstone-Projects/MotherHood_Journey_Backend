package com.motherhood.journey.maternal.service;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.common.repository.AuditLogRepository;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.dto.request.CreatedMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
import com.motherhood.journey.maternal.enums.NidaVerifiedStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E flow: caller (a health worker) registers a mother.
 * Verifies the full chain: geo lookup, facility lookup, health ID generation
 * (MH-YYYY-NNNNNN format), NIDA verification trigger, and audit log entry.
 */
@Transactional
class MotherRegistrationFlowIntegrationTest extends IntegrationTestBase {

    @Autowired MotherService motherService;
    @Autowired UserRepository userRepository;
    @Autowired FacilityRepository facilityRepository;
    @Autowired GeoRepository geoRepository;
    @Autowired AuditLogRepository auditLogRepository;

    private User callerHealthWorker;
    private User motherUser;
    private Facility facility;
    private GeoLocation geo;

    @BeforeEach
    void seed() {
        // Use a geo row seeded by V19__Seed_Geo_Hierarchy.sql
        geo = geoRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V19 geo seed missing"));

        facility = facilityRepository.save(Facility.builder()
            .geoLocation(geo)
            .name("Test Health Center")
            .facilityCode("TEST-" + UUID.randomUUID().toString().substring(0, 6))
            .facilityType(FacilityType.HEALTH_CENTER)
            .district(geo.getDistrict())
            .build());

        callerHealthWorker = userRepository.save(User.builder()
            .nationalId("1" + System.nanoTime())
            .phoneNumber("+250788" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.HEALTH_WORKER)
            .firstName("Hw")
            .lastName("Test")
            .preferredLanguage("rw")
            .facility(facility)
            .geoLocation(geo)
            .build());

        motherUser = userRepository.save(User.builder()
            .nationalId("1" + (System.nanoTime() + 1))
            .phoneNumber("+250789" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.PATIENT)
            .firstName("Mama")
            .lastName("Test")
            .preferredLanguage("rw")
            .geoLocation(geo)
            .build());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                callerHealthWorker.getPhoneNumber(), null,
                List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER"))));
    }

    @Test
    void registerMother_generatesHealthId_andLogsAudit() {
        long auditCountBefore = auditLogRepository.count();

        CreatedMotherRequest request = new CreatedMotherRequest(
            motherUser.getId(),
            facility.getId(),
            "1199012345678901",      // valid Rwanda NID pattern
            geo.getId(),
            LocalDate.of(1995, 4, 12),
            null);

        MotherResponse response = motherService.registerMother(request);

        assertThat(response.healthId())
            .as("Health ID must match MH-YYYY-NNNNNN")
            .matches("^MH-\\d{4}-\\d{6}$");
        assertThat(response.nidaVerifiedStatus()).isEqualTo(NidaVerifiedStatus.PENDING.name());

        // AuditAspect should have written one CREATE row for MOTHER
        assertThat(auditLogRepository.count())
            .as("AuditAspect should persist an audit_log entry for the registration")
            .isGreaterThan(auditCountBefore);
    }
}

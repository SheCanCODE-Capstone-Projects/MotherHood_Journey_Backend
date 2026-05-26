package com.motherhood.journey.government.service;

import com.motherhood.journey.IntegrationTestBase;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.government.dto.request.GovReportRequest;
import com.motherhood.journey.government.dto.response.GovReportResponse;
import com.motherhood.journey.government.enums.ReportType;
import com.motherhood.journey.government.enums.ScopeLevel;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E: government analyst generates an aggregate report.
 * Verifies that GovReportAggregator runs server-side (client-supplied
 * aggregates are ignored), produces the expected coverage keys, and
 * that the resulting record is persisted in gov_reports.
 *
 * This is the data half of the HMIS push pipeline — the next step
 * (Dhis2PayloadBuilder + HMIS outbox) is unit-tested separately and
 * skipped here because it requires an external HMIS endpoint.
 */
@Transactional
class GovReportAggregationIntegrationTest extends IntegrationTestBase {

    @Autowired GovReportService govReportService;
    @Autowired UserRepository userRepository;
    @Autowired GeoRepository geoRepository;

    private User analyst;
    private GeoLocation geo;

    @BeforeEach
    void seed() {
        geo = geoRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("V19 geo seed missing"));

        analyst = userRepository.save(User.builder()
            .nationalId("1" + System.nanoTime())
            .phoneNumber("+250788" + (int) (Math.random() * 900_000 + 100_000))
            .passwordHash("$2a$10$placeholder")
            .role(UserRole.GOVERNMENT_ANALYST)
            .firstName("Analyst").lastName("Test")
            .preferredLanguage("en")
            .geoLocation(geo)
            .build());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                analyst.getPhoneNumber(), null,
                List.of(new SimpleGrantedAuthority("ROLE_GOVERNMENT_ANALYST"))));
    }

    @Test
    void generateVaccinationCoverage_runsServerSideAggregation_andPersists() {
        // Caller passes nonsense aggregates — the impl must ignore them
        // and compute fresh from the database via GovReportAggregator.
        Map<String, Object> bogus = Map.of("administered", 999_999, "due", 1);

        GovReportRequest request = new GovReportRequest(
            ReportType.VACCINATION_COVERAGE,
            YearMonth.now().toString(),                 // e.g. "2026-05"
            ScopeLevel.DISTRICT,
            geo.getId(),
            bogus);

        GovReportResponse response = govReportService.generate(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.reportType()).isEqualTo("VACCINATION_COVERAGE");
        assertThat(response.aggregates())
            .as("Server-side aggregation must populate canonical keys regardless of client input")
            .containsKeys("administered", "due", "coverage_pct");
        assertThat(response.aggregates().get("administered"))
            .as("Aggregator must not echo the client's bogus 999_999")
            .isNotEqualTo(999_999);
    }
}

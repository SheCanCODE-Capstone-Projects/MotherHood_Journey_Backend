package com.motherhood.journey.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FacilitySecurityServiceTest {

    private FacilitySecurityService service;
    private static final UUID FACILITY_A = UUID.randomUUID();
    private static final UUID FACILITY_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FacilitySecurityService();
    }

    private Authentication authWithFacility(String role, UUID facilityId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "user", null, List.of(new SimpleGrantedAuthority(role)));
        auth.setDetails(new FacilityAuthDetails(facilityId));
        return auth;
    }

    @Test
    void healthWorker_matchingFacility_returnsTrue() {
        Authentication auth = authWithFacility("ROLE_HEALTH_WORKER", FACILITY_A);
        assertThat(service.hasAccessToFacility(auth, FACILITY_A)).isTrue();
    }

    @Test
    void healthWorker_differentFacility_returnsFalse() {
        Authentication auth = authWithFacility("ROLE_HEALTH_WORKER", FACILITY_A);
        assertThat(service.hasAccessToFacility(auth, FACILITY_B)).isFalse();
    }

    @Test
    void mohAdmin_anyFacility_returnsTrue() {
        Authentication auth = authWithFacility("ROLE_MOH_ADMIN", FACILITY_A);
        assertThat(service.hasAccessToFacility(auth, FACILITY_B)).isTrue();
    }

    @Test
    void districtOfficer_anyFacility_returnsTrue() {
        Authentication auth = authWithFacility("ROLE_DISTRICT_OFFICER", FACILITY_A);
        assertThat(service.hasAccessToFacility(auth, FACILITY_B)).isTrue();
    }

    @Test
    void nullAuthentication_returnsFalse() {
        assertThat(service.hasAccessToFacility(null, FACILITY_A)).isFalse();
    }

    @Test
    void nullFacilityId_returnsFalse() {
        Authentication auth = authWithFacility("ROLE_HEALTH_WORKER", FACILITY_A);
        assertThat(service.hasAccessToFacility(auth, null)).isFalse();
    }
}

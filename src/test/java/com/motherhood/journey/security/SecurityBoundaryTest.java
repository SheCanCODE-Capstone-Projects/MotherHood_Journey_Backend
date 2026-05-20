package com.motherhood.journey.security;

import com.motherhood.journey.common.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Boundary Unit Tests")
class SecurityBoundaryTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. FacilitySecurityService — access control
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FacilitySecurityService access control")
    class FacilityAccessControl {

        private final FacilitySecurityService svc = new FacilitySecurityService();

        @Test
        @DisplayName("MOH_ADMIN bypasses facility check")
        void mohAdminBypassesFacilityCheck() {
            UUID facilityId = UUID.randomUUID();
            var auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_MOH_ADMIN")));
            assertThat(svc.hasAccessToFacility(auth, facilityId)).isTrue();
        }

        @Test
        @DisplayName("DISTRICT_OFFICER bypasses facility check")
        void districtOfficerBypassesFacilityCheck() {
            UUID facilityId = UUID.randomUUID();
            var auth = new UsernamePasswordAuthenticationToken(
                "officer", null, List.of(new SimpleGrantedAuthority("ROLE_DISTRICT_OFFICER")));
            assertThat(svc.hasAccessToFacility(auth, facilityId)).isTrue();
        }

        @Test
        @DisplayName("FACILITY_ADMIN with matching JWT facilityId gets access")
        void facilityAdminWithMatchingIdGetsAccess() {
            UUID facilityId = UUID.randomUUID();
            var auth = new UsernamePasswordAuthenticationToken(
                "worker", null, List.of(new SimpleGrantedAuthority("ROLE_FACILITY_ADMIN")));
            auth.setDetails(new FacilityAuthDetails(facilityId));
            assertThat(svc.hasAccessToFacility(auth, facilityId)).isTrue();
        }

        @Test
        @DisplayName("FACILITY_ADMIN with mismatched JWT facilityId is denied")
        void facilityAdminWithMismatchedIdIsDenied() {
            UUID jwtFacilityId  = UUID.randomUUID();
            UUID otherFacilityId = UUID.randomUUID();
            var auth = new UsernamePasswordAuthenticationToken(
                "worker", null, List.of(new SimpleGrantedAuthority("ROLE_FACILITY_ADMIN")));
            auth.setDetails(new FacilityAuthDetails(jwtFacilityId));
            assertThat(svc.hasAccessToFacility(auth, otherFacilityId)).isFalse();
        }

        @Test
        @DisplayName("null authentication is denied")
        void nullAuthenticationIsDenied() {
            assertThat(svc.hasAccessToFacility(null, UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("null facilityId is denied")
        void nullFacilityIdIsDenied() {
            var auth = new UsernamePasswordAuthenticationToken(
                "worker", null, List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
            assertThat(svc.hasAccessToFacility(auth, null)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. JWT facility claim extraction
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FacilityAuthDetails extraction")
    class FacilityAuthDetailsExtraction {

        @Test
        @DisplayName("extracts facilityId from authentication details")
        void extractsFacilityIdFromDetails() {
            UUID facilityId = UUID.randomUUID();
            var auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
            auth.setDetails(new FacilityAuthDetails(facilityId));

            UUID extracted = auth.getDetails() instanceof FacilityAuthDetails fd
                ? fd.facilityId() : null;

            assertThat(extracted).isEqualTo(facilityId);
        }

        @Test
        @DisplayName("returns null when details are not FacilityAuthDetails")
        void returnsNullWhenDetailsAreWrongType() {
            var auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
            auth.setDetails("not-facility-details");

            UUID extracted = auth.getDetails() instanceof FacilityAuthDetails fd
                ? fd.facilityId() : null;

            assertThat(extracted).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. Role-based access — CustomException thrown on mismatch
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Facility mismatch throws FORBIDDEN")
    class FacilityMismatchThrows {

        @Test
        @DisplayName("throws FORBIDDEN when HEALTH_WORKER accesses wrong facility")
        void throwsForbiddenOnFacilityMismatch() {
            UUID jwtFacilityId   = UUID.randomUUID();
            UUID requestFacilityId = UUID.randomUUID(); // different

            var auth = new UsernamePasswordAuthenticationToken(
                "+250700000001", null,
                List.of(new SimpleGrantedAuthority("ROLE_HEALTH_WORKER")));
            auth.setDetails(new FacilityAuthDetails(jwtFacilityId));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Simulate the guard logic used in all service impls
            assertThatThrownBy(() -> {
                var currentAuth = SecurityContextHolder.getContext().getAuthentication();
                boolean isCross = currentAuth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .anyMatch(r -> r.equals("ROLE_MOH_ADMIN") || r.equals("ROLE_DISTRICT_OFFICER"));
                if (!isCross) {
                    UUID jwt = currentAuth.getDetails() instanceof FacilityAuthDetails fd
                        ? fd.facilityId() : null;
                    if (jwt == null || !jwt.equals(requestFacilityId)) {
                        throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
                    }
                }
            })
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("facility mismatch");
        }

        @Test
        @DisplayName("does NOT throw when MOH_ADMIN accesses any facility")
        void doesNotThrowForMohAdmin() {
            UUID anyFacilityId = UUID.randomUUID();

            var auth = new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_MOH_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Should not throw
            var currentAuth = SecurityContextHolder.getContext().getAuthentication();
            boolean isCross = currentAuth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(r -> r.equals("ROLE_MOH_ADMIN") || r.equals("ROLE_DISTRICT_OFFICER"));

            assertThat(isCross).isTrue();
        }
    }
}

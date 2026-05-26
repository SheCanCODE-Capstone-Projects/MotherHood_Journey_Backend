package com.motherhood.journey.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component("facilitySecurityService")
public class FacilitySecurityService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of(
        "ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER"
    );

    /**
     * Returns true if the authenticated user has access to the given facilityId.
     * MOH_ADMIN and DISTRICT_OFFICER bypass the facility check.
     * All other roles must have a JWT facilityId that exactly matches the requested facilityId.
     */
    public boolean hasAccessToFacility(Authentication authentication, UUID facilityId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (facilityId == null) {
            return false;
        }

        // Cross-facility roles bypass the check
        boolean isCrossFacilityRole = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);
        if (isCrossFacilityRole) {
            return true;
        }

        Object details = authentication.getDetails();
        if (details instanceof FacilityAuthDetails facilityDetails) {
            UUID jwtFacilityId = facilityDetails.facilityId();
            return jwtFacilityId != null && jwtFacilityId.equals(facilityId);
        }
        return false;
    }
}

package com.motherhood.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("facilitySecurityService")
public class FacilitySecurityService {

    public boolean hasAccessToFacility(Authentication authentication, Long facilityId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Object details = authentication.getDetails();
        if (details instanceof FacilityDetails facilityDetails) {
            return facilityDetails.getFacilityId() != null &&
                   facilityDetails.getFacilityId().equals(facilityId);
        }
        return false;
    }
}

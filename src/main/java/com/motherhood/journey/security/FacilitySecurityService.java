package com.motherhood.journey.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("facilitySecurityService")
public class FacilitySecurityService {

    public boolean hasAccessToFacility(Authentication authentication, Long facilityId) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        Object details = authentication.getDetails();
        if (details instanceof FacilityAuthDetails facilityDetails) {
            return facilityDetails.getFacilityId() != null &&
                   facilityDetails.getFacilityId().equals(facilityId);
        }
        return false;
    }
}

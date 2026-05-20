package com.motherhood.journey.security;

import java.util.UUID;

/**
 * Immutable value object attached to the Authentication.details field by JwtFilter.
 * Carries the facilityId extracted from the JWT claim.
 */
public record FacilityAuthDetails(UUID facilityId) {
}

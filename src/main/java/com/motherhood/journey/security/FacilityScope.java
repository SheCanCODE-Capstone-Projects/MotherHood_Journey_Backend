package com.motherhood.journey.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts method access to users whose JWT facilityId matches the resource's facility.
 * MOH_ADMIN and DISTRICT_OFFICER roles bypass the facility check (cross-facility access).
 *
 * <p><strong>REQUIRED:</strong> The annotated method MUST have a parameter named exactly
 * {@code facilityId} of type {@link java.util.UUID}. The SpEL expression {@code #facilityId}
 * binds to that parameter by name. Renaming the parameter silently disables the security check.
 *
 * <p>Correct usage:
 * <pre>
 *   {@literal @}FacilityScope
 *   public MotherResponse getMotherById(UUID id, UUID facilityId) { ... }
 * </pre>
 *
 * <p>Wrong — security check will NOT fire:
 * <pre>
 *   {@literal @}FacilityScope
 *   public MotherResponse getMotherById(UUID id, UUID fId) { ... }  // parameter renamed!
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize(
    "hasAnyRole('MOH_ADMIN', 'DISTRICT_OFFICER') or " +
    "@facilitySecurityService.hasAccessToFacility(authentication, #facilityId)"
)
public @interface FacilityScope {
}

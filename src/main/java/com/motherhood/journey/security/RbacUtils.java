package com.motherhood.journey.security;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Centralised RBAC + geo-scope checks. Use instead of inlining
 * role/facility/geo guards in services.
 */
public final class RbacUtils {

    private static final Set<String> CROSS_FACILITY_AUTHORITIES =
        Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER", "ROLE_GOVERNMENT_ANALYST");

    private RbacUtils() {}

    public static Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static User currentUser() {
        Authentication auth = currentAuth();
        if (auth == null) return null;
        return auth.getPrincipal() instanceof User u ? u : null;
    }

    public static boolean hasAnyAuthority(String... authorities) {
        Authentication auth = currentAuth();
        if (auth == null) return false;
        for (GrantedAuthority granted : auth.getAuthorities()) {
            for (String wanted : authorities) {
                if (granted.getAuthority().equals(wanted)) return true;
            }
        }
        return false;
    }

    public static boolean isCrossFacility() {
        Authentication auth = currentAuth();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_AUTHORITIES::contains);
    }

    public static UUID currentJwtFacilityId() {
        Authentication auth = currentAuth();
        if (auth == null) return null;
        return auth.getDetails() instanceof FacilityAuthDetails fd ? fd.facilityId() : null;
    }

    public static void assertSameFacility(UUID resourceFacilityId) {
        if (isCrossFacility()) return;
        UUID jwtFacilityId = currentJwtFacilityId();
        if (jwtFacilityId == null || !jwtFacilityId.equals(resourceFacilityId)) {
            throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
        }
    }

    public static void assertGeoScope(UUID resourceGeoId) {
        User user = currentUser();
        if (user == null) return;
        UserRole role = user.getRole();
        if (role != UserRole.DISTRICT_OFFICER) return;
        List<UUID> scope = user.getScopedGeoIds();
        if (scope == null || scope.isEmpty() || !scope.contains(resourceGeoId)) {
            throw new CustomException(
                "Access denied: resource is outside your scoped sectors",
                HttpStatus.FORBIDDEN);
        }
    }
}

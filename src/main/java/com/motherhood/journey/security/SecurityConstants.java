package com.motherhood.journey.security;

import java.util.Set;

/**
 * Shared security constants used across service-layer authorization checks.
 * Centralizes role names to prevent drift when roles are renamed.
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    /**
     * Roles that bypass facility-scoped access checks.
     * These roles have cross-facility read/write access.
     */
    public static final Set<String> CROSS_FACILITY_ROLES = Set.of(
        "ROLE_MOH_ADMIN",
        "ROLE_DISTRICT_OFFICER"
    );
}

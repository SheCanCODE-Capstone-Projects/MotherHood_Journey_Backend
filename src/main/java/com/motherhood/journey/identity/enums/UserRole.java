package com.motherhood.journey.identity.enums;

/**
 * Roles for the MotherHood Journey platform.
 * PATIENT | HEALTH_WORKER | FACILITY_ADMIN | DISTRICT_OFFICER | GOVERNMENT_ANALYST | MOH_ADMIN
 */
public enum UserRole {
    PATIENT,
    HEALTH_WORKER,
    FACILITY_ADMIN,
    DISTRICT_OFFICER,
    GOVERNMENT_ANALYST,
    MOH_ADMIN;

    public String toGrantedAuthority() {
        return "ROLE_" + this.name();
    }
    public boolean isGovernmentRole() {
        return this == DISTRICT_OFFICER
                || this == GOVERNMENT_ANALYST
                || this == MOH_ADMIN;
    }
}


package com.motherhood.journey.admin.dto.response;

public record AdminDashboardResponse(
    long totalFacilities,
    long totalMothers,
    long totalChildren,
    long totalUsers,
    long pendingAppointments,
    long pendingVaccinations,
    long activePregnancies
) {}

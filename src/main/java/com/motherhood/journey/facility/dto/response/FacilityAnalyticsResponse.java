package com.motherhood.journey.facility.dto.response;

import java.util.UUID;

public record FacilityAnalyticsResponse(
    UUID facilityId,
    long activeMothers,
    long ancVisitsLast30Days,
    double ancAttendanceRate,
    long vaccinationsDue,
    long vaccinationsAdministered,
    double vaccinationCoverageRate,
    long appointmentsScheduled,
    long appointmentsNoShow,
    double noShowRate,
    long serviceRequestsPending,
    long serviceRequestsOverdue
) {}

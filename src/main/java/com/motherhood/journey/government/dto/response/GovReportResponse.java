package com.motherhood.journey.government.dto.response;

import com.motherhood.journey.government.entity.GovReport;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record GovReportResponse(
    UUID id,
    UUID generatedById,
    UUID geoLocationId,
    String reportType,
    String period,
    String scopeLevel,
    Map<String, Object> aggregates,
    String hmisPushStatus,
    LocalDateTime generatedAt,
    LocalDateTime pushedAt
) {
    public static GovReportResponse from(GovReport r) {
        return new GovReportResponse(
            r.getId(),
            r.getGeneratedBy().getId(),
            r.getGeoLocation().getId(),
            r.getReportType(),
            r.getPeriod(),
            r.getScopeLevel(),
            r.getAggregates(),
            r.getHmisPushStatus(),
            r.getGeneratedAt(),
            r.getPushedAt()
        );
    }
}

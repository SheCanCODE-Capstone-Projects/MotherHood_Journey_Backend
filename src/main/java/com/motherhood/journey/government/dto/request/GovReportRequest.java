package com.motherhood.journey.government.dto.request;

import com.motherhood.journey.government.enums.ReportType;
import com.motherhood.journey.government.enums.ScopeLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record GovReportRequest(
    @NotNull ReportType reportType,
    @NotNull @Size(max = 16) String period,
    @NotNull ScopeLevel scopeLevel,
    @NotNull UUID geoLocationId,
    @NotNull Map<String, Object> aggregates
) {}

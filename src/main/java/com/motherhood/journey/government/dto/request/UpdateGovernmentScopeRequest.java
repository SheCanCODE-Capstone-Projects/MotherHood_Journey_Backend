package com.motherhood.journey.government.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdateGovernmentScopeRequest(
    List<UUID> scopedGeoIds,
    Boolean canExport,
    Boolean canPushHmis
) {}

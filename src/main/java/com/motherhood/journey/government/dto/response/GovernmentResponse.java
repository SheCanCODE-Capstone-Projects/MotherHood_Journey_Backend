package com.motherhood.journey.government.dto.response;

import com.motherhood.journey.government.entity.GovernmentUser;

import java.time.LocalDateTime;
import java.util.UUID;

public record GovernmentResponse(
    UUID id,
    UUID userId,
    String govRole,
    String ministry,
    String employeeId,
    Boolean canExport,
    Boolean canPushHmis,
    LocalDateTime lastAudit,
    LocalDateTime createdAt
) {
    public static GovernmentResponse from(GovernmentUser g) {
        return new GovernmentResponse(
            g.getId(),
            g.getUser().getId(),
            g.getGovRole(),
            g.getMinistry(),
            g.getEmployeeId(),
            g.getCanExport(),
            g.getCanPushHmis(),
            g.getLastAudit(),
            g.getCreatedAt()
        );
    }
}

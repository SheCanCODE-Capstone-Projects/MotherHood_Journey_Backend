package com.motherhood.journey.government.dto.response;

import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.enums.ServiceRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceRequestResponse(
    UUID id,
    String referenceNo,
    String serviceType,
    ServiceRequestStatus status,
    UUID facilityId,
    UUID requesterId,
    String rejectionReason,
    LocalDateTime submittedAt,
    LocalDateTime resolvedAt
) {
    public static ServiceRequestResponse from(ServiceRequest sr) {
        return new ServiceRequestResponse(
            sr.getId(),
            sr.getReferenceNo(),
            sr.getServiceType(),
            sr.getStatus(),
            sr.getFacility() != null ? sr.getFacility().getId() : null,
            sr.getRequester() != null ? sr.getRequester().getId() : null,
            sr.getRejectionReason(),
            sr.getSubmittedAt(),
            sr.getResolvedAt()
        );
    }
}

package com.motherhood.journey.government.dto.response;

import com.motherhood.journey.government.entity.ServiceRequestDoc;

import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceRequestDocResponse(
    UUID id,
    UUID requestId,
    String documentType,
    String filePath,
    String fileHash,
    LocalDateTime uploadedAt
) {
    public static ServiceRequestDocResponse from(ServiceRequestDoc d) {
        return new ServiceRequestDocResponse(
            d.getId(),
            d.getRequest().getId(),
            d.getDocumentType(),
            d.getFilePath(),
            d.getFileHash(),
            d.getUploadedAt()
        );
    }
}

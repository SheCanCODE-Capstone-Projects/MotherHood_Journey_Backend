package com.motherhood.journey.government.dto.request;

import com.motherhood.journey.government.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttachDocumentRequest(
    @NotNull DocumentType documentType,
    @NotBlank @Size(max = 255) String filePath,
    @NotBlank @Size(max = 64) String fileHash
) {}

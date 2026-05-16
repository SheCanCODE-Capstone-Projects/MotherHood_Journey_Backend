package com.motherhood.journey.child.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateChildRequest(
    @Size(max = 64) String firstName,
    @Size(max = 64) String birthCertificateNo,
    String healthStatus
) {}

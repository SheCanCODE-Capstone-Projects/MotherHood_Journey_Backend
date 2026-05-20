package com.motherhood.journey.identity.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 64)
    String firstName,

    @Size(max = 64)
    String lastName,

    @Size(max = 8)
    String preferredLanguage,

    Boolean active
) {}

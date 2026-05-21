package com.motherhood.journey.identity.dto.request;

import com.motherhood.journey.identity.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegisterRequest(
    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    String phoneNumber,

    @NotBlank(message = "National ID is required")
    @Size(max = 32)
    String nationalId,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "First name is required")
    @Size(max = 64)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 64)
    String lastName,

    @NotNull(message = "Role is required")
    Role role,

    @NotNull(message = "Geo location is required")
    UUID geoLocationId,

    UUID facilityId
) {}

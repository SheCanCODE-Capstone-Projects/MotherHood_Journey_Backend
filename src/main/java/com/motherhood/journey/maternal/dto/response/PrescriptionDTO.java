package com.motherhood.journey.maternal.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * durationDays will be used in a later sprint to schedule
 * medication adherence SMS reminders.
 */
public record PrescriptionDTO(
        UUID id,
        UUID visitId,
        String medicationName,
        String dosage,
        String frequency,
        Integer durationDays,
        String instructions,
        LocalDateTime createdAt
) {}

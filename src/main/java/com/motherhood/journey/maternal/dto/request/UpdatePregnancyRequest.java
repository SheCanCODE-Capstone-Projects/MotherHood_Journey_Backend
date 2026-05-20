package com.motherhood.journey.maternal.dto.request;

import java.time.LocalDate;
import java.util.UUID;

public record UpdatePregnancyRequest(
    LocalDate lmpDate,
    LocalDate edd,
    String status,
    Integer gravida,
    Integer para,
    UUID assignedChwId,
    String outcomeNotes
) {}

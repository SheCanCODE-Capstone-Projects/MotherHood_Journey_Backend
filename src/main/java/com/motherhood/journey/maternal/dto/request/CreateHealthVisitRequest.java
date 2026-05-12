package com.motherhood.journey.maternal.dto.request;

import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.maternal.enums.VisitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateHealthVisitRequest(
    @NotNull
    PatientType patientType,

    @NotNull
    UUID patientRefId,

    @NotNull
    VisitType visitType,

    @NotNull
    Long facilityId,

    @NotNull
    UUID healthWorkerId,

    UUID geoLocationId,

    @NotNull
    LocalDateTime visitDatetime,

    @Size(max = 1000)
    String chiefComplaint,

    Double weightKg,

    Double heightCm,

    Integer systolicBp,

    Integer diastolicBp,

    Double muacCm,

    @Size(max = 2000)
    String notes,

    @Valid
    @NotNull
    @Size(min = 1)
    List<DiagnosisRequest> diagnoses
) {}

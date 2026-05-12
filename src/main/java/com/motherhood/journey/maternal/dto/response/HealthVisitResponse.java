package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.maternal.enums.VisitType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HealthVisitResponse(
    UUID id,
    PatientType patientType,
    UUID patientRefId,
    VisitType visitType,
    Long facilityId,
    String facilityName,
    UUID healthWorkerId,
    String healthWorkerName,
    UUID geoLocationId,
    String geoLocationName,
    LocalDateTime visitDatetime,
    String chiefComplaint,
    Double weightKg,
    Double heightCm,
    Integer systolicBp,
    Integer diastolicBp,
    Double muacCm,
    String notes,
    List<DiagnosisResponse> diagnoses,
    LocalDateTime createdAt
) {}

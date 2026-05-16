package com.motherhood.journey.maternal.dto.request;

import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.maternal.enums.VisitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateHealthVisitRequest(
    @NotNull UUID patientRefId,
    @NotNull PatientType patientType,
    @NotNull UUID facilityId,
    @NotNull UUID healthWorkerId,
    UUID geoLocationId,

    @NotNull
    @PastOrPresent(message = "Visit date cannot be in the future")
    LocalDateTime visitDatetime,

    @NotNull VisitType visitType,

    @Size(max = 500) String chiefComplaint,

    @DecimalMin(value = "0.5",  message = "Weight must be at least 0.5 kg")
    @DecimalMax(value = "300.0", message = "Weight cannot exceed 300 kg")
    Double weightKg,

    @DecimalMin(value = "20.0",  message = "Height must be at least 20 cm")
    @DecimalMax(value = "250.0", message = "Height cannot exceed 250 cm")
    Double heightCm,

    @Min(value = 40,  message = "Systolic BP must be at least 40 mmHg")
    @Max(value = 300, message = "Systolic BP cannot exceed 300 mmHg")
    Integer systolicBp,

    @Min(value = 20,  message = "Diastolic BP must be at least 20 mmHg")
    @Max(value = 200, message = "Diastolic BP cannot exceed 200 mmHg")
    Integer diastolicBp,

    @DecimalMin(value = "5.0",  message = "MUAC must be at least 5 cm")
    @DecimalMax(value = "50.0", message = "MUAC cannot exceed 50 cm")
    Double muacCm,

    @Size(max = 2000) String notes,

    @Valid List<DiagnosisRequest> diagnoses,
    @Valid List<PrescriptionRequest> prescriptions
) {}

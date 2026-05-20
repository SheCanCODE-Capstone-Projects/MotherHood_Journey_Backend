package com.motherhood.journey.maternal.dto.response;

import com.motherhood.journey.maternal.entity.Diagnosis;
import com.motherhood.journey.maternal.entity.HealthVisit;
import com.motherhood.journey.maternal.entity.Prescription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HealthVisitResponse(
    UUID id,
    UUID patientRefId,
    String patientType,
    UUID facilityId,
    UUID healthWorkerId,
    LocalDateTime visitDatetime,
    String visitType,
    String chiefComplaint,
    Double weightKg,
    Double heightCm,
    Integer systolicBp,
    Integer diastolicBp,
    Double muacCm,
    String notes,
    LocalDateTime createdAt,
    List<DiagnosisResponse> diagnoses,
    List<PrescriptionResponse> prescriptions
) {
    public record DiagnosisResponse(
        UUID id,
        String icd10Code,
        String description,
        String severity,
        Boolean isPrimary
    ) {
        public static DiagnosisResponse from(Diagnosis d) {
            return new DiagnosisResponse(d.getId(), d.getIcd10Code(),
                d.getDescription(), d.getSeverity(), d.getIsPrimary());
        }
    }

    public record PrescriptionResponse(
        UUID id,
        String medicationName,
        String dosage,
        String frequency,
        Integer durationDays,
        String instructions
    ) {
        public static PrescriptionResponse from(Prescription p) {
            return new PrescriptionResponse(p.getId(), p.getMedicationName(),
                p.getDosage(), p.getFrequency(), p.getDurationDays(), p.getInstructions());
        }
    }

    public static HealthVisitResponse from(
            HealthVisit v,
            List<Diagnosis> diagnoses,
            List<Prescription> prescriptions) {
        return new HealthVisitResponse(
            v.getId(),
            v.getPatientRefId(),
            v.getPatientType(),
            v.getFacility() != null ? v.getFacility().getId() : null,
            v.getHealthWorker() != null ? v.getHealthWorker().getId() : null,
            v.getVisitDatetime(),
            v.getVisitType() != null ? v.getVisitType().name() : null,
            v.getChiefComplaint(),
            v.getWeightKg(),
            v.getHeightCm(),
            v.getSystolicBp(),
            v.getDiastolicBp(),
            v.getMuacCm(),
            v.getNotes(),
            v.getCreatedAt(),
            diagnoses.stream().map(DiagnosisResponse::from).toList(),
            prescriptions.stream().map(PrescriptionResponse::from).toList()
        );
    }
}

package com.motherhood.journey.maternal.service;

import com.motherhood.journey.child.repository.ChildRepository;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.facility.entity.Facility;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.request.DiagnosisRequest;
import com.motherhood.journey.maternal.dto.response.DiagnosisResponse;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;
import com.motherhood.journey.maternal.entity.Diagnosis;
import com.motherhood.journey.maternal.entity.HealthVisit;
import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.maternal.repository.DiagnosisRepository;
import com.motherhood.journey.maternal.repository.HealthVisitRepository;
import com.motherhood.journey.maternal.repository.MotherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthVisitServiceImpl implements HealthVisitService {

    private final HealthVisitRepository healthVisitRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final GeoRepository geoRepository;
    private final MotherRepository motherRepository;
    private final ChildRepository childRepository;

    @Override
    @Transactional
    public HealthVisitResponse recordVisit(CreateHealthVisitRequest request) {
        // Validate facility exists
        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        // Validate health worker exists
        User healthWorker = userRepository.findById(request.healthWorkerId())
            .orElseThrow(() -> new CustomException("Health worker not found", HttpStatus.NOT_FOUND));

        // Validate geo location if provided
        GeoLocation geoLocation = null;
        if (request.geoLocationId() != null) {
            geoLocation = geoRepository.findById(request.geoLocationId())
                .orElseThrow(() -> new CustomException("Geo location not found", HttpStatus.NOT_FOUND));
        }

        // Validate patient exists based on type
        validatePatientExists(request.patientType(), request.patientRefId());

        // Create health visit
        HealthVisit visit = HealthVisit.builder()
            .patientRefId(request.patientRefId())
            .patientType(request.patientType().name())
            .facility(facility)
            .healthWorker(healthWorker)
            .geoLocation(geoLocation)
            .visitDatetime(request.visitDatetime())
            .visitType(request.visitType().name())
            .chiefComplaint(request.chiefComplaint())
            .weightKg(request.weightKg())
            .heightCm(request.heightCm())
            .systolicBp(request.systolicBp())
            .diastolicBp(request.diastolicBp())
            .muacCm(request.muacCm())
            .notes(request.notes())
            .build();

        HealthVisit savedVisit = healthVisitRepository.save(visit);

        // Create diagnoses
        List<Diagnosis> diagnoses = request.diagnoses().stream()
            .map(diagRequest -> createDiagnosis(savedVisit, diagRequest))
            .collect(Collectors.toList());

        diagnosisRepository.saveAll(diagnoses);

        // Build response
        List<DiagnosisResponse> diagnosisResponses = diagnoses.stream()
            .map(this::mapToDiagnosisResponse)
            .collect(Collectors.toList());

        return mapToHealthVisitResponse(savedVisit, diagnosisResponses);
    }

    @Override
    public HealthVisitResponse getVisitById(UUID id) {
        HealthVisit visit = healthVisitRepository.findById(id)
            .orElseThrow(() -> new CustomException("Health visit not found", HttpStatus.NOT_FOUND));

        List<Diagnosis> diagnoses = diagnosisRepository.findByVisitId(id);
        List<DiagnosisResponse> diagnosisResponses = diagnoses.stream()
            .map(this::mapToDiagnosisResponse)
            .collect(Collectors.toList());

        return mapToHealthVisitResponse(visit, diagnosisResponses);
    }

    private void validatePatientExists(PatientType patientType, UUID patientRefId) {
        boolean exists = switch (patientType) {
            case MOTHER -> motherRepository.existsById(patientRefId);
            case CHILD -> childRepository.existsById(patientRefId);
        };
        if (!exists) {
            String typeName = patientType.name().toLowerCase();
            throw new CustomException(typeName + " not found with ID: " + patientRefId, HttpStatus.NOT_FOUND);
        }
    }

    private Diagnosis createDiagnosis(HealthVisit visit, DiagnosisRequest request) {
        return Diagnosis.builder()
            .visit(visit)
            .icd10Code(request.icd10Code())
            .description(request.description())
            .severity(request.severity())
            .isPrimary(request.isPrimary())
            .build();
    }

    private DiagnosisResponse mapToDiagnosisResponse(Diagnosis diagnosis) {
        return new DiagnosisResponse(
            diagnosis.getId(),
            diagnosis.getIcd10Code(),
            diagnosis.getDescription(),
            diagnosis.getSeverity(),
            diagnosis.getIsPrimary(),
            diagnosis.getCreatedAt()
        );
    }

    private HealthVisitResponse mapToHealthVisitResponse(HealthVisit visit, List<DiagnosisResponse> diagnoses) {
        return new HealthVisitResponse(
            visit.getId(),
            com.motherhood.journey.maternal.enums.PatientType.valueOf(visit.getPatientType()),
            visit.getPatientRefId(),
            com.motherhood.journey.maternal.enums.VisitType.valueOf(visit.getVisitType()),
            visit.getFacility().getId(),
            visit.getFacility().getName(),
            visit.getHealthWorker().getId(),
            visit.getHealthWorker().getFirstName() + " " + visit.getHealthWorker().getLastName(),
            visit.getGeoLocation() != null ? visit.getGeoLocation().getId() : null,
            visit.getGeoLocation() != null ?
                visit.getGeoLocation().getVillage() + ", " +
                visit.getGeoLocation().getCell() + ", " +
                visit.getGeoLocation().getSector() + ", " +
                visit.getGeoLocation().getDistrict() : null,
            visit.getVisitDatetime(),
            visit.getChiefComplaint(),
            visit.getWeightKg(),
            visit.getHeightCm(),
            visit.getSystolicBp(),
            visit.getDiastolicBp(),
            visit.getMuacCm(),
            visit.getNotes(),
            diagnoses,
            visit.getCreatedAt()
        );
    }
}

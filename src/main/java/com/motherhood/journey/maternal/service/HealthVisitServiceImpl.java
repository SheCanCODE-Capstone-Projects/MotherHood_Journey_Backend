package com.motherhood.journey.maternal.service;

import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.request.DiagnosisRequest;
import com.motherhood.journey.maternal.dto.request.PrescriptionRequest;
import com.motherhood.journey.maternal.dto.request.UpdateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;
import com.motherhood.journey.maternal.entity.Diagnosis;
import com.motherhood.journey.maternal.entity.HealthVisit;
import com.motherhood.journey.maternal.entity.Prescription;
import com.motherhood.journey.maternal.repository.DiagnosisRepository;
import com.motherhood.journey.maternal.repository.HealthVisitRepository;
import com.motherhood.journey.maternal.repository.PrescriptionRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import com.motherhood.journey.security.FacilityScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class HealthVisitServiceImpl implements HealthVisitService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private final HealthVisitRepository healthVisitRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public HealthVisitServiceImpl(HealthVisitRepository healthVisitRepository,
                                  DiagnosisRepository diagnosisRepository,
                                  PrescriptionRepository prescriptionRepository,
                                  FacilityRepository facilityRepository,
                                  GeoRepository geoRepository,
                                  UserRepository userRepository,
                                  AuditService auditService) {
        this.healthVisitRepository = healthVisitRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Override
    public HealthVisitResponse createVisit(CreateHealthVisitRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCrossFacility = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);
        if (!isCrossFacility) {
            UUID jwtFacilityId = auth.getDetails() instanceof FacilityAuthDetails fd
                ? fd.facilityId() : null;
            if (jwtFacilityId == null || !jwtFacilityId.equals(request.facilityId())) {
                throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
            }
        }

        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        User healthWorker = userRepository.findById(request.healthWorkerId())
            .orElseThrow(() -> new CustomException("Health worker not found", HttpStatus.NOT_FOUND));

        GeoLocation geoLocation = null;
        if (request.geoLocationId() != null) {
            geoLocation = geoRepository.findById(request.geoLocationId())
                .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));
        }

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

        HealthVisit saved = healthVisitRepository.save(visit);

        List<Diagnosis> savedDiagnoses = List.of();
        List<DiagnosisRequest> diagReqs = request.diagnoses() != null ? request.diagnoses() : List.of();
        if (!diagReqs.isEmpty()) {
            savedDiagnoses = diagnosisRepository.saveAll(diagReqs.stream()
                .map(d -> Diagnosis.builder()
                    .visit(saved)
                    .icd10Code(d.icd10Code())
                    .description(d.description())
                    .severity(d.severity())
                    .isPrimary(d.isPrimary())
                    .build())
                .toList());
        }

        List<Prescription> savedPrescriptions = List.of();
        List<PrescriptionRequest> rxReqs = request.prescriptions() != null ? request.prescriptions() : List.of();
        if (!rxReqs.isEmpty()) {
            savedPrescriptions = prescriptionRepository.saveAll(rxReqs.stream()
                .map(p -> Prescription.builder()
                    .visit(saved)
                    .medicationName(p.medicationName())
                    .dosage(p.dosage())
                    .frequency(p.frequency())
                    .durationDays(p.durationDays())
                    .instructions(p.instructions())
                    .build())
                .toList());
        }

        auditService.log(AuditAction.CREATE, "HEALTH_VISIT", saved.getId());
        return HealthVisitResponse.from(saved, savedDiagnoses, savedPrescriptions);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public HealthVisitResponse getVisitById(UUID id, UUID facilityId) {
        HealthVisit visit = findByIdAndFacility(id, facilityId);
        return HealthVisitResponse.from(
            visit,
            diagnosisRepository.findByVisit_Id(visit.getId()),
            prescriptionRepository.findByVisit_Id(visit.getId())
        );
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public Page<HealthVisitResponse> getVisitsByFacility(UUID facilityId, Pageable pageable) {
        return healthVisitRepository.findByFacility_Id(facilityId, pageable)
            .map(v -> HealthVisitResponse.from(
                v,
                diagnosisRepository.findByVisit_Id(v.getId()),
                prescriptionRepository.findByVisit_Id(v.getId())
            ));
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public Page<HealthVisitResponse> getVisitsByPatient(
            UUID patientRefId, String patientType, UUID facilityId, Pageable pageable) {
        return healthVisitRepository
            .findByPatientRefIdAndPatientTypeAndFacility_Id(patientRefId, patientType, facilityId, pageable)
            .map(v -> HealthVisitResponse.from(
                v,
                diagnosisRepository.findByVisit_Id(v.getId()),
                prescriptionRepository.findByVisit_Id(v.getId())
            ));
    }

    @Override
    @FacilityScope
    public HealthVisitResponse updateVisit(UUID id, UUID facilityId, UpdateHealthVisitRequest request) {
        HealthVisit visit = findByIdAndFacility(id, facilityId);
        if (request.visitDatetime() != null)  visit.setVisitDatetime(request.visitDatetime());
        if (request.visitType() != null)      visit.setVisitType(request.visitType());
        if (request.chiefComplaint() != null) visit.setChiefComplaint(request.chiefComplaint());
        if (request.weightKg() != null)       visit.setWeightKg(request.weightKg());
        if (request.heightCm() != null)       visit.setHeightCm(request.heightCm());
        if (request.systolicBp() != null)     visit.setSystolicBp(request.systolicBp());
        if (request.diastolicBp() != null)    visit.setDiastolicBp(request.diastolicBp());
        if (request.muacCm() != null)         visit.setMuacCm(request.muacCm());
        if (request.notes() != null)          visit.setNotes(request.notes());
        auditService.log(AuditAction.UPDATE, "HEALTH_VISIT", id);
        return HealthVisitResponse.from(
            visit,
            diagnosisRepository.findByVisit_Id(visit.getId()),
            prescriptionRepository.findByVisit_Id(visit.getId())
        );
    }

    private HealthVisit findByIdAndFacility(UUID id, UUID facilityId) {
        return healthVisitRepository.findByIdAndFacility_Id(id, facilityId)
            .orElseThrow(() -> new CustomException("Health visit not found", HttpStatus.NOT_FOUND));
    }
}

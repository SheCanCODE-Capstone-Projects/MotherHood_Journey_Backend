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
import com.motherhood.journey.maternal.enums.PatientType;
import com.motherhood.journey.security.FacilityAuthDetails;
import com.motherhood.journey.security.FacilityScope;
import com.motherhood.journey.security.SecurityConstants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class HealthVisitServiceImpl implements HealthVisitService {

    private static final java.util.Set<String> CROSS_FACILITY_ROLES = SecurityConstants.CROSS_FACILITY_ROLES;


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
        assertFacilityAccess(request.facilityId());

        Facility facility       = resolveFacility(request.facilityId());
        User healthWorker        = resolveHealthWorker(request.healthWorkerId());
        GeoLocation geoLocation  = resolveGeoLocation(request.geoLocationId());

        HealthVisit saved = persistVisit(request, facility, healthWorker, geoLocation);
        List<Diagnosis> diagnoses         = persistDiagnoses(saved, request.diagnoses());
        List<Prescription> prescriptions  = persistPrescriptions(saved, request.prescriptions());

        auditService.log(AuditAction.CREATE, "HEALTH_VISIT", saved.getId());
        return HealthVisitResponse.from(saved, diagnoses, prescriptions);
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
        Page<HealthVisit> visits = healthVisitRepository.findByFacility_Id(facilityId, pageable);
        return buildPageResponse(visits);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public Page<HealthVisitResponse> getVisitsByPatient(
            UUID patientRefId, PatientType patientType, UUID facilityId, Pageable pageable) {
        Page<HealthVisit> visits = healthVisitRepository
            .findByPatientRefIdAndPatientTypeAndFacility_Id(patientRefId, patientType.name(), facilityId, pageable);
        return buildPageResponse(visits);
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

    // ── Private helpers ───────────────────────────────────────────────────────

    private void assertFacilityAccess(UUID requestFacilityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCrossFacility = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);
        if (isCrossFacility) return;
        UUID jwtFacilityId = auth.getDetails() instanceof FacilityAuthDetails fd
            ? fd.facilityId() : null;
        if (jwtFacilityId == null || !jwtFacilityId.equals(requestFacilityId)) {
            throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
        }
    }

    private Facility resolveFacility(UUID facilityId) {
        return facilityRepository.findById(facilityId)
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));
    }

    private User resolveHealthWorker(UUID healthWorkerId) {
        return userRepository.findById(healthWorkerId)
            .orElseThrow(() -> new CustomException("Health worker not found", HttpStatus.NOT_FOUND));
    }

    private GeoLocation resolveGeoLocation(UUID geoLocationId) {
        if (geoLocationId == null) return null;
        return geoRepository.findById(geoLocationId)
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));
    }

    private HealthVisit persistVisit(CreateHealthVisitRequest request,
                                     Facility facility, User healthWorker, GeoLocation geoLocation) {
        return healthVisitRepository.save(HealthVisit.builder()
            .patientRefId(request.patientRefId())
            .patientType(request.patientType().name())
            .facility(facility)
            .healthWorker(healthWorker)
            .geoLocation(geoLocation)
            .visitDatetime(request.visitDatetime())
            .visitType(request.visitType())
            .chiefComplaint(request.chiefComplaint())
            .weightKg(request.weightKg())
            .heightCm(request.heightCm())
            .systolicBp(request.systolicBp())
            .diastolicBp(request.diastolicBp())
            .muacCm(request.muacCm())
            .notes(request.notes())
            .build());
    }

    private List<Diagnosis> persistDiagnoses(HealthVisit visit, List<DiagnosisRequest> diagReqs) {
        if (diagReqs == null || diagReqs.isEmpty()) return List.of();
        return diagnosisRepository.saveAll(diagReqs.stream()
            .map(d -> Diagnosis.builder()
                .visit(visit)
                .icd10Code(d.icd10Code())
                .description(d.description())
                .severity(d.severity())
                .isPrimary(d.isPrimary())
                .build())
            .toList());
    }

    private List<Prescription> persistPrescriptions(HealthVisit visit, List<PrescriptionRequest> rxReqs) {
        if (rxReqs == null || rxReqs.isEmpty()) return List.of();
        return prescriptionRepository.saveAll(rxReqs.stream()
            .map(p -> Prescription.builder()
                .visit(visit)
                .medicationName(p.medicationName())
                .dosage(p.dosage())
                .frequency(p.frequency())
                .durationDays(p.durationDays())
                .instructions(p.instructions())
                .build())
            .toList());
    }

    private HealthVisit findByIdAndFacility(UUID id, UUID facilityId) {
        return healthVisitRepository.findByIdAndFacility_Id(id, facilityId)
            .orElseThrow(() -> new CustomException("Health visit not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Batch-fetches diagnoses and prescriptions for an entire page of visits in 2 queries
     * instead of 2N queries. Eliminates the N+1 problem on paginated endpoints.
     */
    private Page<HealthVisitResponse> buildPageResponse(Page<HealthVisit> visits) {
        List<UUID> visitIds = visits.map(HealthVisit::getId).toList();

        Map<UUID, List<Diagnosis>> diagMap = diagnosisRepository.findByVisit_IdIn(visitIds)
            .stream().collect(Collectors.groupingBy(d -> d.getVisit().getId()));

        Map<UUID, List<Prescription>> rxMap = prescriptionRepository.findByVisit_IdIn(visitIds)
            .stream().collect(Collectors.groupingBy(p -> p.getVisit().getId()));

        return visits.map(v -> HealthVisitResponse.from(
            v,
            diagMap.getOrDefault(v.getId(), List.of()),
            rxMap.getOrDefault(v.getId(), List.of())
        ));
    }
}

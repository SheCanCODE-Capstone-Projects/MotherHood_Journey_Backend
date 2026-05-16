package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.CreateChildRequest;
import com.motherhood.journey.child.dto.request.UpdateChildRequest;
import com.motherhood.journey.child.dto.response.ChildResponse;
import com.motherhood.journey.child.entity.Child;
import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.child.entity.VaccinationSchedule;
import com.motherhood.journey.child.repository.ChildRepository;
import com.motherhood.journey.child.repository.VaccinationRecordRepository;
import com.motherhood.journey.child.repository.VaccinationScheduleRepository;
import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.common.exception.MotherNotFoundException;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.maternal.entity.Mother;
import com.motherhood.journey.maternal.repository.MotherRepository;
import com.motherhood.journey.security.FacilityAuthDetails;
import com.motherhood.journey.security.FacilityScope;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ChildServiceImpl implements ChildService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private final ChildRepository childRepository;
    private final MotherRepository motherRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final VaccinationScheduleRepository vaccinationScheduleRepository;
    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final AuditService auditService;

    public ChildServiceImpl(ChildRepository childRepository,
                            MotherRepository motherRepository,
                            FacilityRepository facilityRepository,
                            GeoRepository geoRepository,
                            VaccinationScheduleRepository vaccinationScheduleRepository,
                            VaccinationRecordRepository vaccinationRecordRepository,
                            AuditService auditService) {
        this.childRepository = childRepository;
        this.motherRepository = motherRepository;
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
        this.vaccinationScheduleRepository = vaccinationScheduleRepository;
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.auditService = auditService;
    }

    @Override
    public ChildResponse registerChild(CreateChildRequest request) {
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

        Mother mother = motherRepository.findById(request.motherId())
            .orElseThrow(() -> new MotherNotFoundException(request.motherId()));

        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));

        Child child = Child.builder()
            .mother(mother)
            .facility(facility)
            .geoLocation(geoLocation)
            .healthId(generateHealthId(request.dateOfBirth(), request.motherId()))
            .birthCertificateNo(request.birthCertificateNo())
            .firstName(request.firstName())
            .gender(request.gender().name())
            .dateOfBirth(request.dateOfBirth())
            .birthWeightKg(request.birthWeightKg())
            .deliveryType(request.deliveryType().name())
            .build();

        Child saved = childRepository.save(child);

        List<VaccinationSchedule> schedules = vaccinationScheduleRepository.findByIsMandatoryTrue();
        List<VaccinationRecord> records = schedules.stream()
            .map(schedule -> VaccinationRecord.builder()
                .child(saved)
                .schedule(schedule)
                .facility(facility)
                .dueDate(request.dateOfBirth().plusDays(schedule.getDueAgeDays()))
                .build())
            .toList();
        vaccinationRecordRepository.saveAll(records);

        auditService.log(AuditAction.CREATE, "CHILD", saved.getId());
        return toResponse(saved);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public ChildResponse getChildById(UUID id, UUID facilityId) {
        Child child = childRepository.findByIdAndFacility_Id(id, facilityId)
            .orElseThrow(() -> new CustomException("Child not found", HttpStatus.NOT_FOUND));
        return toResponse(child);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<ChildResponse> getChildrenByMother(UUID motherId, UUID facilityId) {
        return childRepository.findByMother_IdAndFacility_Id(motherId, facilityId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<ChildResponse> getChildrenByFacility(UUID facilityId) {
        return childRepository.findByFacility_Id(facilityId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @FacilityScope
    public ChildResponse updateChild(UUID id, UUID facilityId, UpdateChildRequest request) {
        Child child = childRepository.findByIdAndFacility_Id(id, facilityId)
            .orElseThrow(() -> new CustomException("Child not found", HttpStatus.NOT_FOUND));
        if (request.firstName() != null)          child.setFirstName(request.firstName());
        if (request.birthCertificateNo() != null) child.setBirthCertificateNo(request.birthCertificateNo());
        if (request.healthStatus() != null)       child.setHealthStatus(request.healthStatus());
        auditService.log(AuditAction.UPDATE, "CHILD", id);
        return toResponse(child);
    }

    private String generateHealthId(LocalDate dateOfBirth, UUID motherId) {
        String date = dateOfBirth.format(DateTimeFormatter.ofPattern("yyyyMMdd", java.util.Locale.ROOT));
        return "CH-" + date + "-" + motherId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    private ChildResponse toResponse(Child child) {
        return new ChildResponse(
            child.getId(),
            child.getHealthId(),
            child.getMother().getId(),
            child.getFacility().getId(),
            child.getGeoLocation().getId(),
            child.getBirthCertificateNo(),
            child.getFirstName(),
            child.getGender(),
            child.getDateOfBirth(),
            child.getBirthWeightKg(),
            child.getDeliveryType(),
            child.getHealthStatus()
        );
    }
}

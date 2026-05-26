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
import com.motherhood.journey.maternal.dto.request.CreateMotherRequest;
import com.motherhood.journey.maternal.dto.request.UpdateMotherRequest;
import com.motherhood.journey.maternal.dto.response.MotherResponse;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class MotherServiceImpl {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private final MotherRepository motherRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;
    private final AuditService auditService;

    public MotherServiceImpl(MotherRepository motherRepository,
                             UserRepository userRepository,
                             FacilityRepository facilityRepository,
                             GeoRepository geoRepository,
                             AuditService auditService) {
        this.motherRepository = motherRepository;
        this.userRepository = userRepository;
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
        this.auditService = auditService;
    }


    public MotherResponse createMother(CreateMotherRequest request) {
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

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        Facility facility = facilityRepository.findById(request.facilityId())
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));

        String healthId = generateHealthId(request.facilityId(), request.userId());

        Mother mother = Mother.builder()
            .user(user)
            .facility(facility)
            .geoLocation(geoLocation)
            .healthId(healthId)
            .dateOfBirth(request.dateOfBirth())
            .educationLevel(request.educationLevel())
            .build();

        Mother saved = motherRepository.save(mother);
        auditService.log(AuditAction.CREATE, "MOTHER", saved.getId());
        return MotherResponse.from(saved);
    }


    @FacilityScope
    @Transactional(readOnly = true)
    public MotherResponse getMotherById(UUID id, UUID facilityId) {
        Mother mother = motherRepository.findById(id)
            .filter(m -> m.getFacility() != null && facilityId.equals(m.getFacility().getId()))
            .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));
        return MotherResponse.from(mother);
    }


    @FacilityScope
    @Transactional(readOnly = true)
    public List<MotherResponse> getMothersByFacility(UUID facilityId) {
        return motherRepository.findByFacilityId(facilityId).stream()
            .map(MotherResponse::from)
            .toList();
    }


    @FacilityScope
    public MotherResponse updateMother(UUID id, UUID facilityId, UpdateMotherRequest request) {
        Mother mother = motherRepository.findById(id)
            .filter(m -> m.getFacility() != null && facilityId.equals(m.getFacility().getId()))
            .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));
        if (request.educationLevel() != null) {
            mother.setEducationLevel(request.educationLevel().name());
        }
        auditService.log(AuditAction.UPDATE, "MOTHER", id);
        return MotherResponse.from(mother);
    }


    @FacilityScope
    public void deactivateMother(UUID id, UUID facilityId) {
        Mother mother = motherRepository.findById(id)
            .filter(m -> m.getFacility() != null && facilityId.equals(m.getFacility().getId()))
            .orElseThrow(() -> new CustomException("Mother not found", HttpStatus.NOT_FOUND));
        if (mother.getUser() != null) {
            mother.getUser().setActive(false);
        }
        auditService.log(AuditAction.DELETE, "MOTHER", id);
    }

    private String generateHealthId(UUID facilityId, UUID userId) {
        return "MTH-" + userId.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }
}

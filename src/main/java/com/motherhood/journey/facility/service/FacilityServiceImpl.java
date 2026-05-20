package com.motherhood.journey.facility.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.request.UpdateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import com.motherhood.journey.geo.entity.Facility;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class FacilityServiceImpl implements FacilityService {

    private static final Set<String> CROSS_FACILITY_ROLES = Set.of("ROLE_MOH_ADMIN", "ROLE_DISTRICT_OFFICER");

    private final FacilityRepository facilityRepository;
    private final GeoRepository geoRepository;

    public FacilityServiceImpl(FacilityRepository facilityRepository, GeoRepository geoRepository) {
        this.facilityRepository = facilityRepository;
        this.geoRepository = geoRepository;
    }

    @Override
    public FacilityResponse createFacility(CreateFacilityRequest request) {
        // FACILITY_ADMIN can only create a facility they are assigned to.
        // MOH_ADMIN can create any facility.
        assertFacilityOwnershipOrMohAdmin(request.facilityId());

        if (facilityRepository.findByFacilityCode(request.facilityCode()).isPresent()) {
            throw new CustomException("Facility code already exists", HttpStatus.CONFLICT);
        }
        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.BAD_REQUEST));

        FacilityType type = parseFacilityType(request.facilityType());

        Facility facility = Facility.builder()
            .name(request.name())
            .facilityCode(request.facilityCode())
            .facilityType(type)
            .district(request.district())
            .phone(request.phone())
            .geoLocation(geoLocation)
            .build();

        return FacilityResponse.from(facilityRepository.save(facility));
    }

    @Override
    @Transactional(readOnly = true)
    public FacilityResponse getFacilityById(UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Facility facility = findById(id);
        boolean isCrossFacility = auth.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);
        if (!isCrossFacility) {
            UUID jwtFacilityId = auth.getDetails() instanceof
                com.motherhood.journey.security.FacilityAuthDetails fd ? fd.facilityId() : null;
            if (jwtFacilityId == null || !jwtFacilityId.equals(facility.getId())) {
                throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
            }
        }
        return FacilityResponse.from(facility);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacilityResponse> getFacilities(String district, FacilityType facilityType, Pageable pageable) {
        if (district != null && facilityType != null) {
            return facilityRepository.findByDistrictAndFacilityTypeAndActiveTrue(district, facilityType, pageable)
                .map(FacilityResponse::from);
        } else if (district != null) {
            return facilityRepository.findByDistrictAndActiveTrue(district, pageable)
                .map(FacilityResponse::from);
        } else if (facilityType != null) {
            return facilityRepository.findByFacilityTypeAndActiveTrue(facilityType, pageable)
                .map(FacilityResponse::from);
        }
        return facilityRepository.findByActiveTrue(pageable).map(FacilityResponse::from);
    }

    @Override
    public FacilityResponse updateFacility(UUID id, UpdateFacilityRequest request) {
        assertFacilityOwnership(id);
        Facility facility = findById(id);
        if (request.name() != null)         facility.setName(request.name());
        if (request.facilityType() != null) facility.setFacilityType(parseFacilityType(request.facilityType()));
        if (request.district() != null)     facility.setDistrict(request.district());
        if (request.phone() != null)        facility.setPhone(request.phone());
        if (request.active() != null)       facility.setActive(request.active());
        return FacilityResponse.from(facility);
    }

    @Override
    public void deleteFacility(UUID id) {
        assertFacilityOwnership(id);
        findById(id).setActive(false);
    }

    private void assertFacilityOwnershipOrMohAdmin(UUID facilityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Only MOH_ADMIN can create new facilities
        boolean isMohAdmin = auth.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .anyMatch("ROLE_MOH_ADMIN"::equals);
        if (!isMohAdmin) {
            throw new CustomException(
                "Access denied: only MOH_ADMIN can create facilities", HttpStatus.FORBIDDEN);
        }
    }

    private void assertFacilityOwnership(UUID facilityId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCrossFacility = auth.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .anyMatch(CROSS_FACILITY_ROLES::contains);
        if (isCrossFacility) return;
        UUID jwtFacilityId = auth.getDetails() instanceof
            com.motherhood.journey.security.FacilityAuthDetails fd ? fd.facilityId() : null;
        if (jwtFacilityId == null || !jwtFacilityId.equals(facilityId)) {
            throw new CustomException("Access denied: facility mismatch", HttpStatus.FORBIDDEN);
        }
    }

    private Facility findById(UUID id) {
        return facilityRepository.findById(id)
            .orElseThrow(() -> new CustomException("Facility not found", HttpStatus.NOT_FOUND));
    }

    private FacilityType parseFacilityType(String value) {
        try {
            return FacilityType.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid facility type: " + value, e, HttpStatus.BAD_REQUEST);
        }
    }
}

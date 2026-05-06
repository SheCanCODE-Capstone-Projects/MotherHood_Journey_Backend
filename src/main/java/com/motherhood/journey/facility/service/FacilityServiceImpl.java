package com.motherhood.journey.facility.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.security.FacilityScope;
import com.motherhood.journey.facility.dto.request.CreateFacilityRequest;
import com.motherhood.journey.facility.dto.request.UpdateFacilityRequest;
import com.motherhood.journey.facility.dto.response.FacilityResponse;
import com.motherhood.journey.facility.entity.Facility;
import com.motherhood.journey.facility.entity.FacilityType;
import com.motherhood.journey.facility.repository.FacilityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityServiceImpl(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    public FacilityResponse createFacility(CreateFacilityRequest request) {
        Facility facility = new Facility(
            request.name(),
            request.district(),
            request.province(),
            request.type(),
            request.phoneNumber(),
            request.latitude(),
            request.longitude()
        );

        Facility savedFacility = facilityRepository.save(facility);
        return mapToResponse(savedFacility);
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public FacilityResponse getFacilityById(Long facilityId) {
        Long id = facilityId;
        Facility facility = facilityRepository.findById(id)
            .orElseThrow(() -> new CustomException("Facility not found with ID: " + id, HttpStatus.NOT_FOUND));
        return mapToResponse(facility);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<FacilityResponse> getFacilitiesByDistrict(String district) {
        return facilityRepository.findByDistrict(district)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<FacilityResponse> getFacilitiesByType(FacilityType type) {
        return facilityRepository.findByType(type)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @FacilityScope
    @Transactional(readOnly = true)
    public List<FacilityResponse> getFacilitiesByDistrictAndType(String district, FacilityType type) {
        return facilityRepository.findByDistrictAndType(district, type)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    public FacilityResponse updateFacility(Long id, UpdateFacilityRequest request) {
        Facility facility = facilityRepository.findById(id)
            .orElseThrow(() -> new CustomException("Facility not found with ID: " + id, HttpStatus.NOT_FOUND));

        // Update only non-null fields
        if (request.name() != null) facility.setName(request.name());
        if (request.district() != null) facility.setDistrict(request.district());
        if (request.province() != null) facility.setProvince(request.province());
        if (request.type() != null) facility.setType(request.type());
        if (request.phoneNumber() != null) facility.setPhoneNumber(request.phoneNumber());
        if (request.latitude() != null) facility.setLatitude(request.latitude());
        if (request.longitude() != null) facility.setLongitude(request.longitude());

        Facility updatedFacility = facilityRepository.save(facility);
        return mapToResponse(updatedFacility);
    }

    @Override
    public void deleteFacility(Long id) {
        Facility facility = facilityRepository.findById(id)
            .orElseThrow(() -> new CustomException("Facility not found with ID: " + id, HttpStatus.NOT_FOUND));
        facilityRepository.delete(facility);
    }

    /**
     * Helper method to map Facility entity to FacilityResponse DTO
     */
    private FacilityResponse mapToResponse(Facility facility) {
        return new FacilityResponse(
            facility.getId(),
            facility.getName(),
            facility.getDistrict(),
            facility.getProvince(),
            facility.getType(),
            facility.getPhoneNumber(),
            facility.getLatitude(),
            facility.getLongitude(),
            facility.getCreatedAt(),
            facility.getUpdatedAt()
        );
    }
}


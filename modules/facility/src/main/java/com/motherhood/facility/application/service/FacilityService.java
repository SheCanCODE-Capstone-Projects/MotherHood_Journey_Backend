package com.motherhood.facility.application.service;

import com.motherhood.facility.application.dto.FacilityRequest;
import com.motherhood.facility.application.dto.FacilityResponse;
import com.motherhood.facility.domain.model.Facility;
import com.motherhood.facility.domain.repository.FacilityRepository;
import com.motherhood.facility.infrastructure.mapper.FacilityMapper;
import com.motherhood.shared.exception.ResourceNotFoundException;
import com.motherhood.shared.rbac.FacilityScope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityMapper facilityMapper;

    public FacilityService(FacilityRepository facilityRepository, FacilityMapper facilityMapper) {
        this.facilityRepository = facilityRepository;
        this.facilityMapper = facilityMapper;
    }

    public FacilityResponse create(FacilityRequest request) {
        return facilityMapper.toResponse(facilityRepository.save(facilityMapper.toDomain(request)));
    }

    @FacilityScope
    public FacilityResponse findById(Long facilityId) {
        return facilityRepository.findById(facilityId)
            .map(facilityMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Facility", facilityId));
    }

    @FacilityScope
    public List<FacilityResponse> findByFilters(Long facilityId, String district, Facility.Type type) {
        if (district != null && type != null) {
            return facilityRepository.findByDistrictAndType(district, type)
                .stream().map(facilityMapper::toResponse).toList();
        } else if (district != null) {
            return facilityRepository.findByDistrict(district)
                .stream().map(facilityMapper::toResponse).toList();
        } else if (type != null) {
            return facilityRepository.findByType(type)
                .stream().map(facilityMapper::toResponse).toList();
        }
        return facilityRepository.findAll().stream().map(facilityMapper::toResponse).toList();
    }
}

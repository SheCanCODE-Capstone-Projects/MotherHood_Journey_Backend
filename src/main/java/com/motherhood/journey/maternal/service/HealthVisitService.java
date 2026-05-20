package com.motherhood.journey.maternal.service;

import com.motherhood.journey.maternal.dto.request.CreateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.request.UpdateHealthVisitRequest;
import com.motherhood.journey.maternal.dto.response.HealthVisitResponse;
import com.motherhood.journey.maternal.enums.PatientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface HealthVisitService {
    HealthVisitResponse createVisit(CreateHealthVisitRequest request);
    HealthVisitResponse getVisitById(UUID id, UUID facilityId);
    Page<HealthVisitResponse> getVisitsByFacility(UUID facilityId, Pageable pageable);
    Page<HealthVisitResponse> getVisitsByPatient(UUID patientRefId, PatientType patientType, UUID facilityId, Pageable pageable);
    HealthVisitResponse updateVisit(UUID id, UUID facilityId, UpdateHealthVisitRequest request);
}

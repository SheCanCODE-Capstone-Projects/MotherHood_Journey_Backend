package com.motherhood.journey.child.service;

import com.motherhood.journey.child.dto.request.AdministerVaccinationRequest;
import com.motherhood.journey.child.dto.response.VaccinationRecordResponse;

import java.util.List;
import java.util.UUID;

public interface VaccinationService {
    List<VaccinationRecordResponse> getByChild(UUID childId, UUID facilityId);
    VaccinationRecordResponse administer(UUID recordId, UUID facilityId, AdministerVaccinationRequest request);
    void markOverdueAndNotify();
}
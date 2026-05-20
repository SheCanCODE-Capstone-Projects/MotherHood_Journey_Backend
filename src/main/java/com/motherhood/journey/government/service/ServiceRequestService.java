package com.motherhood.journey.government.service;

import com.motherhood.journey.government.dto.request.SubmitServiceRequestRequest;
import com.motherhood.journey.government.dto.response.ServiceRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceRequestService {
    ServiceRequestResponse submit(SubmitServiceRequestRequest request);
    ServiceRequestResponse getById(UUID id);
    Page<ServiceRequestResponse> getByFacility(UUID facilityId, Pageable pageable);
    Page<ServiceRequestResponse> getByStatus(String status, Pageable pageable);
    ServiceRequestResponse approve(UUID id);
    ServiceRequestResponse reject(UUID id, String reason);
}

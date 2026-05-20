package com.motherhood.journey.government.service;

import com.motherhood.journey.government.dto.request.GovReportRequest;
import com.motherhood.journey.government.dto.response.GovReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GovReportService {
    GovReportResponse generate(GovReportRequest request);
    GovReportResponse getById(UUID id);
    Page<GovReportResponse> getByUser(UUID userId, Pageable pageable);
}

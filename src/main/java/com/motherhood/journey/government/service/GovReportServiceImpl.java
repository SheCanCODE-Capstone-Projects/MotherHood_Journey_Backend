package com.motherhood.journey.government.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import com.motherhood.journey.government.dto.request.GovReportRequest;
import com.motherhood.journey.government.dto.response.GovReportResponse;
import com.motherhood.journey.government.entity.GovReport;
import com.motherhood.journey.government.repository.GovReportRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GovReportServiceImpl implements GovReportService {

    private final GovReportRepository govReportRepository;
    private final UserRepository userRepository;
    private final GeoRepository geoRepository;

    public GovReportServiceImpl(GovReportRepository govReportRepository,
                                UserRepository userRepository,
                                GeoRepository geoRepository) {
        this.govReportRepository = govReportRepository;
        this.userRepository = userRepository;
        this.geoRepository = geoRepository;
    }

    @Override
    public GovReportResponse generate(GovReportRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User generator = userRepository.findByPhoneNumber(auth.getName())
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.UNAUTHORIZED));

        GeoLocation geoLocation = geoRepository.findById(request.geoLocationId())
            .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND));

        GovReport report = GovReport.builder()
            .generatedBy(generator)
            .geoLocation(geoLocation)
            .reportType(request.reportType().name())
            .period(request.period())
            .scopeLevel(request.scopeLevel().name())
            .aggregates(request.aggregates())
            .build();

        return GovReportResponse.from(govReportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    public GovReportResponse getById(UUID id) {
        return GovReportResponse.from(
            govReportRepository.findById(id)
                .orElseThrow(() -> new CustomException("Report not found", HttpStatus.NOT_FOUND))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GovReportResponse> getByUser(UUID userId, Pageable pageable) {
        return govReportRepository.findByGeneratedBy_Id(userId, pageable)
            .map(GovReportResponse::from);
    }
}

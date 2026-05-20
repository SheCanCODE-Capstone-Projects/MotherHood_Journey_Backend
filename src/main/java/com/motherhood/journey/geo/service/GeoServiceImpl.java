package com.motherhood.journey.geo.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.geo.dto.request.CreateGeoRequest;
import com.motherhood.journey.geo.dto.response.GeoResponse;
import com.motherhood.journey.geo.entity.GeoLocation;
import com.motherhood.journey.geo.repository.GeoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GeoServiceImpl implements GeoService {

    private final GeoRepository geoRepository;

    public GeoServiceImpl(GeoRepository geoRepository) {
        this.geoRepository = geoRepository;
    }

    @Override
    public GeoResponse createGeoLocation(CreateGeoRequest request) {
        GeoLocation geo = GeoLocation.builder()
            .province(request.province())
            .district(request.district())
            .sector(request.sector())
            .cell(request.cell())
            .village(request.village())
            .postalCode(request.postalCode())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .build();
        return GeoResponse.from(geoRepository.save(geo));
    }

    @Override
    @Transactional(readOnly = true)
    public GeoResponse getGeoLocationById(UUID id) {
        return GeoResponse.from(
            geoRepository.findById(id)
                .orElseThrow(() -> new CustomException("GeoLocation not found", HttpStatus.NOT_FOUND))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GeoResponse> getAllGeoLocations(Pageable pageable) {
        return geoRepository.findAll(pageable).map(GeoResponse::from);
    }
}

package com.motherhood.journey.geo.service;

import com.motherhood.journey.geo.dto.request.CreateGeoRequest;
import com.motherhood.journey.geo.dto.response.GeoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GeoService {
    GeoResponse createGeoLocation(CreateGeoRequest request);
    GeoResponse getGeoLocationById(UUID id);
    Page<GeoResponse> getAllGeoLocations(Pageable pageable);
}

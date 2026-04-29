package com.motherhood.journey.geo.service;

import com.motherhood.journey.geo.repository.GeoLocationRepository;
import org.springframework.stereotype.Service;

@Service
public class GeoServiceImpl implements GeoLocationService {
    private final GeoLocationRepository geoLocationRepository;

    public GeoServiceImpl(GeoLocationRepository geoLocationRepository) {
        this.geoLocationRepository = geoLocationRepository;
    }
}

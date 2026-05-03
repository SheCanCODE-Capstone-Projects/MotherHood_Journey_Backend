package com.motherhood.journey.geo.service;

import com.motherhood.journey.geo.repository.GeoRepository;
import org.springframework.stereotype.Service;

@Service
public class GeoServiceImpl implements GeoService {
    private final GeoRepository geoRepository;

    public GeoServiceImpl(GeoRepository geoRepository) {
        this.geoRepository = geoRepository;
    }
}

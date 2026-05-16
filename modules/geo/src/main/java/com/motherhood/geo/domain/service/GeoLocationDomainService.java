package com.motherhood.geo.domain.service;

import com.motherhood.geo.domain.model.GeoLocation;
import org.springframework.stereotype.Service;

@Service
public class GeoLocationDomainService {

    // Business rule: a location is only valid if it is active
    public boolean isActive(GeoLocation geoLocation) {
        return geoLocation.getActive() != null
                && geoLocation.getActive();
    }

    // Business rule: all five hierarchy levels must be present
    public boolean isComplete(GeoLocation geoLocation) {
        return geoLocation.getProvince() != null
                && geoLocation.getDistrict() != null
                && geoLocation.getSector() != null
                && geoLocation.getCell() != null
                && geoLocation.getVillage() != null;
    }
}
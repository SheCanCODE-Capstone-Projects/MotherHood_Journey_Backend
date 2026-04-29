package com.motherhood.facility.domain.repository;

import com.motherhood.facility.domain.model.Facility;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository {
    Facility save(Facility facility);
    Optional<Facility> findById(Long id);
    List<Facility> findAll();
    List<Facility> findByDistrict(String district);
    List<Facility> findByType(Facility.Type type);
    List<Facility> findByDistrictAndType(String district, Facility.Type type);
}

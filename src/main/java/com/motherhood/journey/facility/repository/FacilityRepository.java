package com.motherhood.journey.facility.repository;

import com.motherhood.journey.facility.entity.Facility;
import com.motherhood.journey.facility.entity.FacilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByDistrict(String district);
    List<Facility> findByType(FacilityType type);
    List<Facility> findByDistrictAndType(String district, FacilityType type);
    List<Facility> findByProvince(String province);

    Page<Facility> findByDistrict(String district, Pageable pageable);
    Page<Facility> findByType(FacilityType type, Pageable pageable);
    Page<Facility> findByDistrictAndType(String district, FacilityType type, Pageable pageable);
}


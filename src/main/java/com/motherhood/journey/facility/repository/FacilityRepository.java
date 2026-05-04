package com.motherhood.journey.facility.repository;

import com.motherhood.journey.facility.entity.Facility;
import com.motherhood.journey.facility.entity.FacilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByDistrict(String district);

    List<Facility> findByType(FacilityType type);

    List<Facility> findByDistrictAndType(String district, FacilityType type);

    List<Facility> findByProvince(String province);
}


package com.motherhood.facility.infrastructure.persistence;

import com.motherhood.facility.domain.model.Facility;
import com.motherhood.facility.domain.repository.FacilityRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaFacilityRepository extends JpaRepository<Facility, Long>, FacilityRepository {
    List<Facility> findByDistrict(String district);
    List<Facility> findByType(Facility.Type type);
    List<Facility> findByDistrictAndType(String district, Facility.Type type);
}

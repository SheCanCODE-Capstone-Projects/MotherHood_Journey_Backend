package com.motherhood.journey.geo.repository;

import com.motherhood.journey.geo.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID> {
}

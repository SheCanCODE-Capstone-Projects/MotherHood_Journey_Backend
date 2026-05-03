package com.motherhood.journey.geo.repository;

import com.motherhood.journey.geo.entity.GeoLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GeoRepository extends JpaRepository<GeoLocation, UUID> {
}

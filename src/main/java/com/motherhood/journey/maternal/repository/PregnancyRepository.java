package com.motherhood.journey.maternal.repository;

import com.motherhood.journey.maternal.entity.Pregnancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PregnancyRepository extends JpaRepository<Pregnancy, UUID> {

    List<Pregnancy> findByMotherId(UUID motherId);

    @Query(value = """
            SELECT p.* FROM pregnancies p
            JOIN mothers m ON p.mother_id = m.id
            WHERE p.mother_id = :motherId AND m.facility_id = :facilityId
            """, nativeQuery = true)
    List<Pregnancy> findByMotherIdAndFacilityId(@Param("motherId") UUID motherId,
                                                @Param("facilityId") UUID facilityId);

    @Query(value = """
            SELECT p.* FROM pregnancies p
            JOIN mothers m ON p.mother_id = m.id
            WHERE p.id = :id AND m.facility_id = :facilityId
            """, nativeQuery = true)
    Optional<Pregnancy> findByIdAndFacilityId(@Param("id") UUID id,
                                              @Param("facilityId") UUID facilityId);

    Optional<Pregnancy> findFirstByMotherIdAndStatusOrderByCreatedAtDesc(UUID motherId, String status);

    long countByStatus(String status);
}

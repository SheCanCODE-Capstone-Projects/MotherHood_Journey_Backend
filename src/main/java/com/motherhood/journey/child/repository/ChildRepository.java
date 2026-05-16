package com.motherhood.journey.child.repository;

import com.motherhood.journey.child.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findByMother_Id(UUID motherId);

    List<Child> findByFacility_Id(UUID facilityId);

    List<Child> findByMother_IdAndFacility_Id(UUID motherId, UUID facilityId);

    Optional<Child> findByIdAndFacility_Id(UUID id, UUID facilityId);
}

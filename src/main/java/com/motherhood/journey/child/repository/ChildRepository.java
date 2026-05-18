package com.motherhood.journey.child.repository;

import com.motherhood.journey.child.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findByMotherId(UUID motherId);

    List<Child> findByFacilityId(UUID facilityId);

    boolean existsByBirthCertificateNo(String birthCertificateNo);
}

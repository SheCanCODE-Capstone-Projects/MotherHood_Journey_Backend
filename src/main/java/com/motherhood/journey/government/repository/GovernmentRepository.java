package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.GovernmentUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GovernmentRepository extends JpaRepository<GovernmentUser, UUID> {

    Optional<GovernmentUser> findByUser_Id(UUID userId);

    Optional<GovernmentUser> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);
}

package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.GovernmentUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GovernmentRepository extends JpaRepository<GovernmentUser, UUID> {
}

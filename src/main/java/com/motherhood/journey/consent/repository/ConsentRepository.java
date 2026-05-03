package com.motherhood.journey.consent.repository;

import com.motherhood.journey.consent.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<ConsentRecord, UUID> {
}

package com.motherhood.journey.consent.repository;

import com.motherhood.journey.consent.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {
}

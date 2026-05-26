package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.GovSyncLog;
import com.motherhood.journey.government.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GovSyncLogRepository extends JpaRepository<GovSyncLog, UUID> {

    // Fetch all rows ready to be processed (not in-flight, not done, retry time reached)
    List<GovSyncLog> findByStatusAndNextRetryAtLessThanEqualAndDeadLetterFalse(
            SyncStatus status, OffsetDateTime now);

    // Find all DEAD_LETTER rows for admin monitoring
    List<GovSyncLog> findByDeadLetterTrue();

    // Idempotency check before inserting a new entry
    Optional<GovSyncLog> findByIdempotencyKey(String idempotencyKey);

    // For the monitoring endpoint
    List<GovSyncLog> findByStatusOrderByCreatedAtDesc(SyncStatus status);

    long countByStatus(SyncStatus status);

    long countByDeadLetterTrue();
}

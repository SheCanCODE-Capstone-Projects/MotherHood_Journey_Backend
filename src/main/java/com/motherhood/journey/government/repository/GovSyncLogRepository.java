package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.GovSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GovSyncLogRepository extends JpaRepository<GovSyncLog, UUID> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Returns PENDING entries that are either never retried (nextRetryAt IS NULL)
     * or whose next retry time has passed. Limits to 50 per run to avoid overload.
     */
    @Query("""
        SELECT g FROM GovSyncLog g
        WHERE g.status = 'PENDING'
          AND (g.nextRetryAt IS NULL OR g.nextRetryAt <= :now)
        ORDER BY g.createdAt ASC
        LIMIT 50
        """)
    List<GovSyncLog> findPendingForRetry(LocalDateTime now);
}

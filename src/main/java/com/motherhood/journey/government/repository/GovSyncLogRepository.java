package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.GovSyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GovSyncLogRepository extends JpaRepository<GovSyncLog, UUID> {

    List<GovSyncLog> findByStatus(String status);

    List<GovSyncLog> findByTargetSystem(String targetSystem);

    List<GovSyncLog> findByStatusAndTargetSystem(String status, String targetSystem);
}

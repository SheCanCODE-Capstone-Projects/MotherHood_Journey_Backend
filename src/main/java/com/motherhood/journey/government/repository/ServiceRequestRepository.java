package com.motherhood.journey.government.repository;

import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.enums.ServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    Optional<ServiceRequest> findByReferenceNo(String referenceNo);
    Page<ServiceRequest> findByFacility_Id(UUID facilityId, Pageable pageable);
    Page<ServiceRequest> findByRequester_Id(UUID requesterId, Pageable pageable);
    Page<ServiceRequest> findByStatus(ServiceRequestStatus status, Pageable pageable);
    boolean existsByReferenceNo(String referenceNo);
    long countByReferenceNoStartingWith(String prefix);

    /**
     * Finds PENDING requests submitted before the given cutoff — used by escalation scheduler.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT sr FROM ServiceRequest sr
        WHERE sr.status = com.motherhood.journey.government.enums.ServiceRequestStatus.PENDING
          AND sr.submittedAt < :cutoff
        ORDER BY sr.submittedAt ASC
        """)
    java.util.List<ServiceRequest> findStalePendingBefore(
        @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff
    );
}

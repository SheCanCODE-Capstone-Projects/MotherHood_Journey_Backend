package com.motherhood.journey.scheduler;

import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.enums.ServiceRequestStatus;
import com.motherhood.journey.government.repository.ServiceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Escalation scheduler for ServiceRequests.
 *
 * Runs every hour. Any request that has been PENDING for longer than
 * ESCALATION_HOURS (default 48h) is automatically moved to UNDER_REVIEW
 * and flagged for DISTRICT_OFFICER attention via a WARN log.
 *
 * In a full implementation, this would also send an SMS/email to the
 * assigned DISTRICT_OFFICER. The notification hook is marked with TODO.
 */
@Component
public class ServiceRequestEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestEscalationScheduler.class);

    @Value("${app.escalation.pending-hours:48}")
    private int escalationHours;

    private final ServiceRequestRepository serviceRequestRepository;
    private final AuditService auditService;

    public ServiceRequestEscalationScheduler(ServiceRequestRepository serviceRequestRepository,
                                              AuditService auditService) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelay = 3_600_000) // every hour
    @Transactional
    public void escalateStaleRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(escalationHours);
        List<ServiceRequest> stale = serviceRequestRepository.findStalePendingBefore(cutoff);

        if (stale.isEmpty()) return;

        log.warn("EscalationScheduler: escalating {} stale PENDING request(s) older than {}h",
            stale.size(), escalationHours);

        for (ServiceRequest sr : stale) {
            sr.setStatus(ServiceRequestStatus.UNDER_REVIEW);

            // TODO: notify assigned DISTRICT_OFFICER via SMS/notification service
            log.warn("EscalationScheduler: SR {} [{}] escalated to UNDER_REVIEW — " +
                "submitted at {} — route to DISTRICT_OFFICER",
                sr.getReferenceNo(), sr.getId(), sr.getSubmittedAt());

            auditService.log(AuditAction.UPDATE, "SERVICE_REQUEST", sr.getId());
        }

        serviceRequestRepository.saveAll(stale);
    }
}

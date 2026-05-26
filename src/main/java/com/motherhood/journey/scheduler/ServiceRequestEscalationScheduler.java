package com.motherhood.journey.scheduler;

import com.motherhood.journey.common.enums.AuditAction;
import com.motherhood.journey.common.service.AuditService;
import com.motherhood.journey.government.entity.ServiceRequest;
import com.motherhood.journey.government.enums.ServiceRequestStatus;
import com.motherhood.journey.government.repository.ServiceRequestRepository;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.repository.NotificationRepository;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
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
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public ServiceRequestEscalationScheduler(ServiceRequestRepository serviceRequestRepository,
                                              AuditService auditService,
                                              NotificationRepository notificationRepository,
                                              UserRepository userRepository) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.auditService = auditService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedDelay = 3_600_000) // every hour
    @Transactional
    public void escalateStaleRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(escalationHours);
        List<ServiceRequest> stale = serviceRequestRepository.findStalePendingBefore(cutoff);

        if (stale.isEmpty()) {
            return;
        }

        log.warn("EscalationScheduler: escalating {} stale PENDING request(s) older than {}h",
            stale.size(), escalationHours);

        for (ServiceRequest sr : stale) {
            sr.setStatus(ServiceRequestStatus.UNDER_REVIEW);

            log.warn("EscalationScheduler: SR {} [{}] escalated to UNDER_REVIEW — " +
                "submitted at {} — routing to DISTRICT_OFFICER",
                sr.getReferenceNo(), sr.getId(), sr.getSubmittedAt());

            notifyDistrictOfficer(sr);
            auditService.log(AuditAction.UPDATE, "SERVICE_REQUEST", sr.getId());
        }

        serviceRequestRepository.saveAll(stale);
    }

    private void notifyDistrictOfficer(ServiceRequest sr) {
        String district = sr.getFacility() != null ? sr.getFacility().getDistrict() : null;
        java.util.Optional<User> officerOpt = userRepository
            .findByRole(com.motherhood.journey.identity.enums.UserRole.DISTRICT_OFFICER)
            .stream()
            .filter(u -> u.getGeoLocation() != null
                && district != null
                && district.equals(u.getGeoLocation().getDistrict()))
            .findFirst();
        officerOpt.ifPresentOrElse(
            officer -> {
                String message = String.format(
                    "[ESCALATION] Service request %s has been pending for over %dh. " +
                    "Please review immediately.", sr.getReferenceNo(), escalationHours);
                SmsNotification sms = SmsNotification.builder()
                    .recipientUser(officer)
                    .phoneNumber(officer.getPhoneNumber())
                    .messageBody(message)
                    .notificationType("ESCALATION_ALERT")
                    .scheduledAt(LocalDateTime.now())
                    .build();
                notificationRepository.save(sms);
                log.info("EscalationScheduler: SMS queued for DISTRICT_OFFICER {} re SR {}",
                    officer.getId(), sr.getReferenceNo());
            },
            () -> log.warn("EscalationScheduler: no DISTRICT_OFFICER found for SR {} — " +
                "notification skipped", sr.getReferenceNo())
        );
    }
}

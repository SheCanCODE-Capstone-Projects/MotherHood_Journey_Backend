package com.motherhood.journey.scheduler;

import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;

    public ReminderScheduler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Processes all QUEUED notifications whose scheduledAt is in the past.
     * Runs every 60 seconds. Marks each as SENT on success or increments
     * retryCount and marks FAILED after MAX_RETRIES attempts.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processQueuedNotifications() {
        List<SmsNotification> due = notificationRepository.findDueNotifications(LocalDateTime.now());
        if (due.isEmpty()) return;

        log.info("ReminderScheduler: processing {} due notification(s)", due.size());

        for (SmsNotification notification : due) {
            try {
                // TODO: inject AfricasTalking SMS client and call send API here
                // africasTalkingClient.send(notification.getPhoneNumber(), notification.getMessageBody());
                notification.setStatus("SENT");
                notification.setSentAt(LocalDateTime.now());
                log.debug("Notification {} marked SENT to {}", notification.getId(), notification.getPhoneNumber());
            } catch (Exception e) {
                int retries = notification.getRetryCount() + 1;
                notification.setRetryCount(retries);
                if (retries >= MAX_RETRIES) {
                    notification.setStatus("FAILED");
                    log.warn("Notification {} FAILED after {} retries", notification.getId(), retries, e);
                } else {
                    log.warn("Notification {} retry {}/{}", notification.getId(), retries, MAX_RETRIES, e);
                }
            }
        }

        notificationRepository.saveAll(due);
    }
}

package com.motherhood.journey.scheduler;

import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.child.service.VaccinationService;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final int MAX_RETRIES = 3;

    private final VaccinationService vaccinationService;
    private final AppointmentService appointmentService;
    private final NotificationRepository notificationRepository;

    // 01:00 Rwanda time — flip overdue vaccinations and enqueue SMS
    @Scheduled(cron = "0 0 1 * * *", zone = "Africa/Kigali")
    public void scanOverdueVaccinations() {
        log.info("Starting nightly overdue vaccination scan...");
        vaccinationService.markOverdueAndNotify();
    }

    // 08:00 Rwanda time — send appointment reminders for next 24h window
    @Scheduled(cron = "0 0 8 * * *", zone = "Africa/Kigali")
    public void sendAppointmentReminders() {
        log.info("Starting daily appointment reminder scan...");
        appointmentService.sendUpcomingReminders();
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
            } catch (RuntimeException e) {
                int retries = notification.getRetryCount() + 1;
                notification.setRetryCount(retries);
                if (retries >= MAX_RETRIES) {
                    notification.setStatus("FAILED");
                    log.error("Notification {} FAILED after {} retries: {}", notification.getId(), retries, e.getMessage(), e);
                } else {
                    log.warn("Notification {} retry {}/{}: {}", notification.getId(), retries, MAX_RETRIES, e.getMessage());
                }
            }
        }

        notificationRepository.saveAll(due);
    }
}
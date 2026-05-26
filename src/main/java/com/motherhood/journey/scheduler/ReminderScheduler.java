package com.motherhood.journey.scheduler;

import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.child.service.VaccinationService;
import com.motherhood.journey.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final VaccinationService vaccinationService;
    private final AppointmentService appointmentService;
    private final NotificationService notificationService;

    // 01:00 Rwanda time — flip overdue vaccinations and enqueue SMS
    @Scheduled(cron = "0 0 1 * * *", zone = "Africa/Kigali")
    @SchedulerLock(name = "scanOverdueVaccinations", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
    public void scanOverdueVaccinations() {
        log.info("Starting nightly overdue vaccination scan...");
        vaccinationService.markOverdueAndNotify();
    }

    // 08:00 Rwanda time — send appointment reminders for next 24h window
    @Scheduled(cron = "0 0 8 * * *", zone = "Africa/Kigali")
    @SchedulerLock(name = "sendAppointmentReminders", lockAtLeastFor = "PT1M", lockAtMostFor = "PT30M")
    public void sendAppointmentReminders() {
        log.info("Starting daily appointment reminder scan...");
        appointmentService.sendUpcomingReminders();
    }

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "processQueuedNotifications", lockAtLeastFor = "PT30S", lockAtMostFor = "PT5M")
    public void processQueuedNotifications() {
        notificationService.processQueuedNotifications();
    }
}
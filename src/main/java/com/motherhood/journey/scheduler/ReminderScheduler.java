package com.motherhood.journey.scheduler;

import com.motherhood.journey.appointment.service.AppointmentService;
import com.motherhood.journey.child.service.VaccinationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final VaccinationService vaccinationService;
    private final AppointmentService appointmentService;

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
}
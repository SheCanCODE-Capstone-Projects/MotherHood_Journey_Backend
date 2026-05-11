package com.motherhood.journey.scheduler;

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

    @Scheduled(cron = "0 0 1 * * *", zone = "Africa/Kigali")
    public void scanOverdueVaccinations() {
        log.info("Starting nightly overdue vaccination scan...");
        vaccinationService.markOverdueAndNotify();
    }
}
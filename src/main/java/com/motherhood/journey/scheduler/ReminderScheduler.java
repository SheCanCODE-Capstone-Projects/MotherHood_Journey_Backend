package com.motherhood.journey.scheduler;

import com.motherhood.journey.government.service.GovSyncService;
import com.motherhood.journey.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

	private final GovSyncService govSyncService;
	private final NotificationService notificationService;

	/**
	 * Processes pending government API sync entries every 2 minutes.
	 */
	@Scheduled(fixedDelay = 120_000) // 2 minutes
	public void runGovSyncProcessor() {
		log.info("ReminderScheduler: running gov sync outbox processor");
		govSyncService.processPendingEntries();
	}

	/**
	 * Sends queued SMS notifications every 5 minutes.
	 */
	@Scheduled(fixedDelay = 300_000) // 5 minutes
	public void runNotificationProcessor() {
		log.info("ReminderScheduler: running notification queue processor");
		notificationService.processQueuedNotifications();
	}
}

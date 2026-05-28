package com.motherhood.journey.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the gov_sync_log outbox every 30 seconds and dispatches pending entries
 * to their target systems (NIDA / HMIS / IREMBO).
 *
 * All transactional work is done in GovSyncOutboxDelegate to ensure Spring's
 * @Transactional proxy is applied correctly (self-calls bypass the proxy).
 */
@Component
public class GovSyncOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(GovSyncOutboxProcessor.class);

    private final GovSyncOutboxDelegate delegate;

    public GovSyncOutboxProcessor(GovSyncOutboxDelegate delegate) {
        this.delegate = delegate;
    }

    @Scheduled(fixedDelay = 30_000)
    @SchedulerLock(name = "govSyncOutboxProcess", lockAtLeastFor = "PT10S", lockAtMostFor = "PT5M")
    public void process() {
        var pending = delegate.fetchAndMarkInProgress();
        if (pending.isEmpty()) {
            return;
        }

        log.info("GovSyncOutboxProcessor: processing {} pending entry(ies)", pending.size());
        pending.forEach(delegate::dispatch);
    }
}

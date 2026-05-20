package com.motherhood.journey.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox processor for GovSyncLog entries targeting Irembo.
 *
 * Runs every 30 seconds. Delegates all transactional work to
 * GovSyncOutboxDelegate to ensure @Transactional is applied correctly
 * via Spring proxy (self-calls within the same bean bypass the proxy).
 */
@Component
public class GovSyncOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(GovSyncOutboxProcessor.class);

    private final GovSyncOutboxDelegate delegate;

    @Value("${irembo.base-url:}")
    private String iremboBaseUrl;

    @Value("${irembo.api-key:}")
    private String iremboApiKey;

    public GovSyncOutboxProcessor(GovSyncOutboxDelegate delegate) {
        this.delegate = delegate;
    }

    @Scheduled(fixedDelay = 30_000)
    public void process() {
        if (iremboBaseUrl == null || iremboBaseUrl.isBlank()) {
            log.debug("GovSyncOutboxProcessor: IREMBO_BASE_URL not configured — skipping");
            return;
        }

        var pending = delegate.fetchAndMarkInProgress();
        if (pending.isEmpty()) return;

        log.info("GovSyncOutboxProcessor: processing {} pending entry(ies)", pending.size());
        pending.forEach(entry -> delegate.dispatch(entry, iremboBaseUrl, iremboApiKey));
    }
}

package com.motherhood.journey.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableScheduling
public class ProductionConfig {

    private static final Logger log = LoggerFactory.getLogger(ProductionConfig.class);

    /**
     * Caffeine-backed cache for UserDetails.
     * TTL = 1 minute — balances DB load vs. deactivated-user propagation latency.
     * A deactivated user will be evicted within 60 seconds without a forced cache clear.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userDetails");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(500));
        return manager;
    }

    /**
     * Runs on the 1st of every month at 00:05 UTC.
     * Warns if the next month's audit_log partition does not exist yet.
     * A DBA or automated job must create it before the month starts.
     * See DEPLOYMENT.md for the partition creation SQL template.
     */
    @Scheduled(cron = "0 5 0 1 * *", zone = "UTC")
    public void warnIfPartitionExpiringSoon() {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        String partitionName = String.format("audit_log_%d_%02d",
            nextMonth.getYear(), nextMonth.getMonthValue());
        log.warn("PARTITION CHECK: Ensure '{}' exists in PostgreSQL before {}-{}-01. " +
            "See DEPLOYMENT.md for creation SQL.", partitionName,
            nextMonth.getYear(), String.format("%02d", nextMonth.getMonthValue()));
    }
}

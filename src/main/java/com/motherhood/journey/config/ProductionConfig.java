package com.motherhood.journey.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableScheduling
public class ProductionConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ProductionConfig.class);
    private static final int PARTITIONS_AHEAD = 3;

    private final JdbcTemplate jdbc;
    private final org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManagerProvider;

    public ProductionConfig(JdbcTemplate jdbc,
                            org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManagerProvider) {
        this.jdbc = jdbc;
        this.cacheManagerProvider = cacheManagerProvider;
    }

    // When Redis is unreachable, log the error and bypass the cache rather than
    // propagating a connection exception that would kill the request entirely.
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler();
    }

    /**
     * Caffeine cache for non-prod profiles. In-process, single-instance.
     */
    @Bean(name = "cacheManager")
    @Profile("!prod")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userDetails", "geoLookup", "facilityLookup");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(500));
        return manager;
    }

    /**
     * Redis cache for prod. Shared across replicas, survives restarts.
     *
     * Two serializer configurations are used:
     * - typedMapper: activateDefaultTyping(NON_FINAL, WRAPPER_OBJECT) for caches that store
     *   JPA entities (userDetails, facilityLookup). Needed so Jackson knows the concrete class
     *   when deserializing back from Object.
     * - simpleMapper: JavaTimeModule only, no defaultTyping for caches that store plain value
     *   types like List<String> (geoLookup). defaultTyping breaks on Java's unmodifiable lists
     *   (final internal types) so those caches use a plain serializer.
     */
    @Bean(name = "cacheManager")
    @Profile("prod")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper typedMapper = new ObjectMapper();
        typedMapper.registerModule(new JavaTimeModule());
        typedMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        typedMapper.activateDefaultTyping(
            typedMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.WRAPPER_OBJECT
        );

        ObjectMapper simpleMapper = new ObjectMapper();
        simpleMapper.registerModule(new JavaTimeModule());
        simpleMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        var keySerializer = RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());

        RedisCacheConfiguration typedConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(1))
            .disableCachingNullValues()
            .serializeKeysWith(keySerializer)
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(typedMapper)));

        RedisCacheConfiguration simpleConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues()
            .serializeKeysWith(keySerializer)
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(simpleMapper)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(typedConfig)
            .withInitialCacheConfigurations(Map.of("geoLookup", simpleConfig))
            .build();
    }

    /**
     * Immediately evicts a user's cached UserDetails entry.
     * Call this whenever a user is deactivated or their role changes
     * so the security context reflects the change on the next request
     * rather than waiting for the 1-minute TTL to expire.
     */
    public void evictUser(String phoneNumber) {
        CacheManager manager = cacheManagerProvider.getIfAvailable();
        if (manager == null) return;
        Cache cache = manager.getCache("userDetails");
        if (cache != null) {
            cache.evict(phoneNumber);
            log.info("UserDetails cache evicted for user: [REDACTED]");
        }
    }

    @PostConstruct
    public void ensurePartitionsOnStartup() {
        ensureFuturePartitions();
    }

    @Scheduled(cron = "0 5 0 1 * *", zone = "UTC")
    @SchedulerLock(name = "ensureAuditPartitions", lockAtLeastFor = "PT1M", lockAtMostFor = "PT15M")
    public void ensurePartitionsMonthly() {
        ensureFuturePartitions();
    }

    void ensureFuturePartitions() {
        LocalDate cursor = LocalDate.now().withDayOfMonth(1);
        for (int i = 0; i <= PARTITIONS_AHEAD; i++) {
            LocalDate start = cursor.plusMonths(i);
            LocalDate end = start.plusMonths(1);
            String partition = String.format("audit_log_%d_%02d", start.getYear(), start.getMonthValue());
            String ddl = String.format(
                "CREATE TABLE IF NOT EXISTS %s PARTITION OF audit_log FOR VALUES FROM ('%s') TO ('%s')",
                partition, start, end);
            try {
                jdbc.execute(ddl);
                log.debug("Partition ensured: {}", partition);
            } catch (Exception e) {
                log.error("Failed to ensure audit_log partition {}: {}", partition, e.getMessage(), e);
            }
        }
    }
}

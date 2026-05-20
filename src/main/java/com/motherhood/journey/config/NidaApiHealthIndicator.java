package com.motherhood.journey.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

@Component("nidaApi")
public class NidaApiHealthIndicator implements HealthIndicator {

    @Value("${nida.base-url:}")
    private String nidaBaseUrl;

    @Override
    public Health health() {
        if (nidaBaseUrl == null || nidaBaseUrl.isBlank()) {
            return Health.unknown().withDetail("reason", "NIDA_BASE_URL not configured").build();
        }
        try {
            HttpURLConnection conn = (HttpURLConnection)
                URI.create(nidaBaseUrl + "/health").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code < 500
                ? Health.up().withDetail("provider", "nida").build()
                : Health.down().withDetail("httpStatus", code).build();
        } catch (IOException e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}

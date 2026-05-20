package com.motherhood.journey.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

@Component("africasTalking")
public class AfricasTalkingHealthIndicator implements HealthIndicator {

    @Value("${africas-talking.api-key}")
    private String apiKey;

    @Override
    public Health health() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_api_key")) {
            return Health.unknown().withDetail("reason", "AT_API_KEY not configured").build();
        }
        try {
            HttpURLConnection conn = (HttpURLConnection)
                URI.create("https://api.africastalking.com/version1/user").toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code < 500
                ? Health.up().withDetail("provider", "africas-talking").build()
                : Health.down().withDetail("httpStatus", code).build();
        } catch (IOException e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}

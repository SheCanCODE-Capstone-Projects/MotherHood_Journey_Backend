package com.motherhood.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.motherhood")
@EnableJpaRepositories(basePackages = "com.motherhood")
@EntityScan(basePackages = "com.motherhood")
@EnableAsync
public class MotherHoodJourneyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotherHoodJourneyApplication.class, args);
    }
}
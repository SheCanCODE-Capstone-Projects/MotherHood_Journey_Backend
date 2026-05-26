package com.motherhood.journey.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SmsTemplateServiceTest {

    private SmsTemplateService service;

    @BeforeEach
    void setUp() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:messages");
        ms.setDefaultEncoding(StandardCharsets.UTF_8.name());
        ms.setUseCodeAsDefaultMessage(true);
        ms.setFallbackToSystemLocale(false);
        MessageSource source = ms;
        service = new SmsTemplateService(source);
    }

    @Test
    void render_returnsEnglishForEnLocale() {
        String message = service.render("sms.vaccination.overdue", "en", "BCG", "2026-04-01");
        assertThat(message).containsIgnoringCase("vaccination overdue");
        assertThat(message).contains("BCG");
        assertThat(message).contains("2026-04-01");
    }

    @Test
    void render_returnsKinyarwandaForRwLocale() {
        String message = service.render("sms.vaccination.overdue", "rw", "BCG", "2026-04-01");
        assertThat(message).contains("Urukingo");
        assertThat(message).contains("BCG");
    }

    @Test
    void render_returnsFrenchForFrLocale() {
        String message = service.render("sms.appointment.reminder", "fr", "2026-05-01", "10:00");
        assertThat(message).containsIgnoringCase("rendez-vous");
    }

    @Test
    void render_fallsBackToDefaultLocaleWhenLanguageBlank() {
        String message = service.render("sms.vaccination.overdue", "", "BCG", "2026-04-01");
        // default locale is "rw"
        assertThat(message).contains("Urukingo");
    }

    @Test
    void render_returnsKeyWhenMessageMissing() {
        String message = service.render("sms.unknown.key", "en");
        assertThat(message).isEqualTo("sms.unknown.key");
    }
}

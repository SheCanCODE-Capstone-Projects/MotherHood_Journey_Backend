package com.motherhood.journey.notification.service;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SmsTemplateService {

    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("rw");

    private final MessageSource messageSource;

    public SmsTemplateService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String render(String key, String preferredLanguage, Object... args) {
        Locale locale = preferredLanguage != null && !preferredLanguage.isBlank()
            ? Locale.forLanguageTag(preferredLanguage)
            : DEFAULT_LOCALE;
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return messageSource.getMessage(key, args, DEFAULT_LOCALE);
        }
    }
}

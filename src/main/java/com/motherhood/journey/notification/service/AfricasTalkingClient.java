package com.motherhood.journey.notification.service;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AfricasTalkingClient {

    @Value("${africas-talking.api-key}")
    private String apiKey;

    @Value("${africas-talking.username}")
    private String username;

    @Value("${africas-talking.sender-id:}")
    private String senderId;

    @PostConstruct
    void init() {
        AfricasTalking.initialize(username, apiKey);
        log.info("Africa's Talking SDK initialized — username={} senderId={}",
            username, senderId.isBlank() ? "(default)" : senderId);
    }

    /**
     * Sends a single SMS and returns the provider message ID when available.
     * Phone numbers are normalised to E.164 (+250 prefix for Rwandan 07xx numbers).
     */
    public String sendSms(String to, String message) {
        try {
            SmsService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
            String from = (senderId != null && !senderId.isBlank()) ? senderId : null;
            var recipients = from != null
                ? sms.send(message, from, new String[]{normalizePhone(to)}, true)
                : sms.send(message, new String[]{normalizePhone(to)}, true);

            if (recipients != null && !recipients.isEmpty()) {
                var first = recipients.getFirst();
                String messageId = first.messageId;
                if (messageId != null && !messageId.isBlank()) {
                    return messageId;
                }
            }

            log.warn("Africa's Talking response did not include messageId for recipient {}", to);
            return null;
        } catch (Exception e) {
            throw new RuntimeException("AFRICAS_TALKING_ERROR: " + e.getMessage(), e);
        }
    }

    /** Converts local Rwandan format (07xx) to E.164 (+25x7xx). */
    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        if (phone.startsWith("+")) return phone;
        if (phone.startsWith("0")) return "+250" + phone.substring(1);
        return "+" + phone;
    }
}

package com.motherhood.journey.notification.service;

import com.motherhood.journey.notification.dto.request.DeliveryWebhookRequest;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.enums.NotificationStatus;
import com.motherhood.journey.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final NotificationRepository notificationRepository;

    // Used to validate the HMAC signature
    @Value("${africas-talking.api-key}")
    private String apiKey;

    // Validates that the request is genuinely from Africa's Talking

    public boolean isValidSignature(String requestBody,
                                    String receivedSignature) {
        if (receivedSignature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    apiKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    requestBody.getBytes(StandardCharsets.UTF_8));

            String calculatedSignature = Base64.getEncoder()
                    .encodeToString(hash);

            return MessageDigest.isEqual(
                    calculatedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error("Failed to validate HMAC signature: {}",
                    e.getMessage());
            return false;
        }
    }

    // Finds the SMS record by at_message_id and updates its status
    // to DELIVERED or FAILED based on what Africa's Talking reported

    @Transactional
    public void handleDeliveryUpdate(DeliveryWebhookRequest request) {

        // Find the SMS notification by Africa's Talking message ID
        Optional<SmsNotification> optional = notificationRepository
                .findByAtMessageId(request.getId());

        if (optional.isEmpty()) {
            log.warn("Webhook received for unknown at_message_id: {}",
                    request.getId());
            return;
        }

        SmsNotification notification = optional.get();

        // Map Africa's Talking status to our internal status
        String atStatus = request.getStatus();

        if ("Success".equalsIgnoreCase(atStatus)) {
            // SMS was successfully delivered to the phone
            notification.setStatus(NotificationStatus.DELIVERED.name());
            log.info("SMS delivered: id={} phone={}",
                    notification.getId(),
                    notification.getPhoneNumber());

        } else if ("Failed".equalsIgnoreCase(atStatus)
                || "Rejected".equalsIgnoreCase(atStatus)) {
            // SMS failed to deliver
            notification.setStatus(NotificationStatus.FAILED.name());
            log.warn("SMS delivery failed: id={} phone={} reason={}",
                    notification.getId(),
                    notification.getPhoneNumber(),
                    request.getFailureReason());

        } else {
            // Unknown status — log and do nothing
            log.warn("Unknown delivery status from AT: status={}",
                    atStatus);
            return;
        }

        notificationRepository.save(notification);
    }
}
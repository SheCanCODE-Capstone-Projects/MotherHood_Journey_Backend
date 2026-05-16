package com.motherhood.journey.notification.dto.response;

import com.motherhood.journey.notification.entity.SmsNotification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID recipientUserId,
    String phoneNumber,
    String messageBody,
    String notificationType,
    String status,
    LocalDateTime scheduledAt,
    LocalDateTime sentAt,
    Integer retryCount,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(SmsNotification n) {
        return new NotificationResponse(
            n.getId(),
            n.getRecipientUser().getId(),
            n.getPhoneNumber(),
            n.getMessageBody(),
            n.getNotificationType(),
            n.getStatus(),
            n.getScheduledAt(),
            n.getSentAt(),
            n.getRetryCount(),
            n.getCreatedAt()
        );
    }
}

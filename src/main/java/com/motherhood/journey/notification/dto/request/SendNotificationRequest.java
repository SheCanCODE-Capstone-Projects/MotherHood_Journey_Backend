package com.motherhood.journey.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record SendNotificationRequest(

    @NotNull(message = "Recipient user ID is required")
    UUID recipientUserId,

    @NotBlank(message = "Phone number is required")
    String phoneNumber,

    @NotBlank(message = "Message body is required")
    String messageBody,

    @NotBlank(message = "Notification type is required")
    String notificationType,

    @NotNull(message = "Scheduled time is required")
    LocalDateTime scheduledAt
) {}

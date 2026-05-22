package com.motherhood.journey.notification.dto.response;

import com.motherhood.journey.notification.enums.NotificationStatus;
import com.motherhood.journey.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID recipientUserId;
    private String phoneNumber;
    private String messageBody;
    private NotificationType notificationType;
    private NotificationStatus status;
    private String atMessageId;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private Integer retryCount;
    private LocalDateTime createdAt;
}
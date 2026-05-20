package com.motherhood.journey.notification.dto.request;

import com.motherhood.journey.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {

	private UUID recipientUserId;
	private String phoneNumber;
	private String message;
	private NotificationType notificationType;
	private LocalDateTime scheduledAt;
}

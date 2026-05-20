package com.motherhood.journey.notification.service;

import com.motherhood.journey.notification.dto.request.SendNotificationRequest;
import com.motherhood.journey.notification.dto.response.NotificationResponse;

public interface NotificationService {

	NotificationResponse enqueue(SendNotificationRequest request);

	void processQueuedNotifications();

	void sendAdminAlert(String message);
}

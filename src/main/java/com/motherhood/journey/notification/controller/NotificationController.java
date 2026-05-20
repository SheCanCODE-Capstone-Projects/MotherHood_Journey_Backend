package com.motherhood.journey.notification.controller;

import com.motherhood.journey.notification.dto.request.SendNotificationRequest;
import com.motherhood.journey.notification.dto.response.NotificationResponse;
import com.motherhood.journey.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> enqueue(@RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notificationService.enqueue(request));
    }

    @PostMapping("/process-queue")
    public ResponseEntity<Void> processQueue() {
        notificationService.processQueuedNotifications();
        return ResponseEntity.accepted().build();
    }
}

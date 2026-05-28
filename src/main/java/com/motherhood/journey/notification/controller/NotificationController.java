package com.motherhood.journey.notification.controller;

import com.motherhood.journey.notification.dto.request.SendNotificationRequest;
import com.motherhood.journey.notification.dto.response.NotificationResponse;
import com.motherhood.journey.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('MOH_ADMIN', 'FACILITY_ADMIN')")
@Tag(name = "Notifications", description = "SMS queueing and queue management — MOH_ADMIN and FACILITY_ADMIN only")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @Operation(
        summary = "Enqueue an outbound SMS notification",
        description = "Queues an SMS for delivery to the specified recipient. "
            + "The scheduler sends it within the next processing cycle (typically 1 minute). "
            + "Restricted to MOH_ADMIN and FACILITY_ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "403", description = "Insufficient role"),
        @ApiResponse(responseCode = "404", description = "Recipient user not found")
    })
    public ResponseEntity<NotificationResponse> enqueue(@Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(notificationService.enqueue(request));
    }

    @PostMapping("/process-queue")
    @PreAuthorize("hasRole('MOH_ADMIN')")
    @Operation(
        summary = "Manually trigger the notification queue processor",
        description = "Processes all QUEUED SMS notifications immediately. "
            + "Normally handled by the scheduled task every minute. "
            + "Restricted to MOH_ADMIN."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Queue processing started"),
        @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public ResponseEntity<Void> processQueue() {
        notificationService.processQueuedNotifications();
        return ResponseEntity.accepted().build();
    }
}

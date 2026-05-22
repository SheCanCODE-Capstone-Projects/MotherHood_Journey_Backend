package com.motherhood.journey.notification.service;

import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.enums.UserRole;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.notification.dto.request.SendNotificationRequest;
import com.motherhood.journey.notification.dto.response.NotificationResponse;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.enums.NotificationStatus;
import com.motherhood.journey.notification.enums.NotificationType;
import com.motherhood.journey.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AfricasTalkingClient africasTalkingClient;

    // Queues a new SMS notification to be sent
    @Override
    @Transactional
    public NotificationResponse enqueue(SendNotificationRequest request) {

        User recipient = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Recipient user not found"));

        // Use provided phone number or fall back to user's phone
        String phone = request.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            phone = recipient.getPhoneNumber();
        }

        SmsNotification notification = SmsNotification.builder()
                .recipientUser(recipient)
                .phoneNumber(phone)
                .messageBody(request.getMessage())
                .notificationType(request.getNotificationType().name())
                .status(NotificationStatus.QUEUED.name())
                .scheduledAt(request.getScheduledAt() == null
                        ? LocalDateTime.now()
                        : request.getScheduledAt())
                .retryCount(0)
                .build();

        SmsNotification saved = notificationRepository.save(notification);
        return convertToResponse(saved);
    }

    // Called by the scheduler every minute
    @Override
    @Transactional
    public void processQueuedNotifications() {

        List<SmsNotification> queued = notificationRepository
                .findDueNotifications(LocalDateTime.now());

        if (queued.isEmpty()) {
            return;
        }

        log.info("Processing {} queued SMS notifications", queued.size());

        for (SmsNotification notification : queued) {
            try {
                String atMessageId = africasTalkingClient.sendSms(
                        notification.getPhoneNumber(),
                        notification.getMessageBody()
                );

                notification.setStatus(NotificationStatus.SENT.name());
                notification.setSentAt(LocalDateTime.now());
                notification.setAtMessageId(atMessageId);
                notificationRepository.save(notification);

            } catch (Exception ex) {

                int nextRetry = notification.getRetryCount() + 1;
                notification.setRetryCount(nextRetry);

                if (nextRetry >= MAX_RETRIES) {
                    notification.setStatus(
                            NotificationStatus.FAILED.name());
                    log.error("SMS permanently failed: id={} error={}",
                            notification.getId(), ex.getMessage());
                } else {
                    long backoffMinutes = (long) Math.pow(2, nextRetry);
                    notification.setScheduledAt(
                            LocalDateTime.now().plusMinutes(backoffMinutes));
                    log.warn("SMS failed: id={} retry={}/{} next={}m",
                            notification.getId(), nextRetry,
                            MAX_RETRIES, backoffMinutes);
                }

                notificationRepository.save(notification);
            }
        }
    }

    // Sends emergency alert to all MOH_ADMIN users
    @Override
    @Transactional
    public void sendAdminAlert(String message) {

        List<User> admins = userRepository
                .findByRoleAndActiveTrue(UserRole.MOH_ADMIN.name());

        if (admins.isEmpty()) {
            log.warn("No active MOH_ADMIN users found. Alert: {}",
                    message);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<SmsNotification> alerts = admins.stream()
                .map(admin -> SmsNotification.builder()
                        .recipientUser(admin)
                        .phoneNumber(admin.getPhoneNumber())
                        .messageBody(message)
                        .notificationType(
                                NotificationType.EMERGENCY.name())
                        .status(NotificationStatus.QUEUED.name())
                        .scheduledAt(now)
                        .retryCount(0)
                        .build())
                .toList();

        notificationRepository.saveAll(alerts);
        log.info("Queued admin alert for {} MOH_ADMIN users",
                alerts.size());
    }

    // Converts SmsNotification entity to NotificationResponse DTO
    private NotificationResponse convertToResponse(
            SmsNotification notification) {

        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientUserId(
                        notification.getRecipientUser() == null
                                ? null
                                : notification.getRecipientUser().getId())
                .phoneNumber(notification.getPhoneNumber())
                .messageBody(notification.getMessageBody())
                .notificationType(NotificationType.valueOf(
                        notification.getNotificationType()))
                .status(NotificationStatus.valueOf(
                        notification.getStatus()))
                .atMessageId(notification.getAtMessageId())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .retryCount(notification.getRetryCount())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
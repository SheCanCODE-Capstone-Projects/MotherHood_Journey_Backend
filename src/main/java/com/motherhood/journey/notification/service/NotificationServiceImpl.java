package com.motherhood.journey.notification.service;

import com.motherhood.journey.common.exception.CustomException;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.identity.repository.UserRepository;
import com.motherhood.journey.notification.dto.request.SendNotificationRequest;
import com.motherhood.journey.notification.dto.response.NotificationResponse;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public NotificationResponse send(SendNotificationRequest request) {
        User recipient = userRepository.findById(request.recipientUserId())
            .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        SmsNotification notification = SmsNotification.builder()
            .recipientUser(recipient)
            .phoneNumber(request.phoneNumber())
            .messageBody(request.messageBody())
            .notificationType(request.notificationType())
            .scheduledAt(request.scheduledAt())
            .build();

        return NotificationResponse.from(notificationRepository.save(notification));
    }
}

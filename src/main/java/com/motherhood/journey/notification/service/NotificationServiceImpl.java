package com.motherhood.journey.notification.service;

import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.enums.NotificationStatus;
import com.motherhood.journey.notification.enums.NotificationType;
import com.motherhood.journey.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    static final int MAX_RETRIES = 3;

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void enqueue(VaccinationRecord record) {
        String phone     = record.getChild().getMother().getUser().getPhoneNumber();
        String vaccine   = record.getSchedule().getVaccineName();
        String childName = record.getChild().getFirstName();

        String message = String.format(
                "%s: %s dose is overdue. Visit facility soon.",
                childName, vaccine
        );

        SmsNotification sms = SmsNotification.builder()
                .recipientUser(record.getChild().getMother().getUser())
                .phoneNumber(phone)
                .messageBody(message)
                .notificationType(NotificationType.VACCINATION_REMINDER.name())
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(sms);
        log.info("SMS enqueued for phone {} — vaccine: {}", phone, vaccine);
    }

    @Override
    @Transactional
    public void enqueueRaw(User recipient, String message, NotificationType type) {
        SmsNotification sms = SmsNotification.builder()
                .recipientUser(recipient)
                .phoneNumber(recipient.getPhoneNumber())
                .messageBody(message)
                .notificationType(type.name())
                .scheduledAt(LocalDateTime.now())
                .build();

        notificationRepository.save(sms);
        log.info("SMS enqueued for phone {} — type: {}", recipient.getPhoneNumber(), type);
    }

    @Override
    @Transactional
    public void retry(SmsNotification sms) {
        if (sms.getRetryCount() >= MAX_RETRIES) {
            sms.setStatus(NotificationStatus.FAILED.name());
            log.warn("SMS {} exceeded max retries — marked FAILED", sms.getId());
        } else {
            sms.setRetryCount(sms.getRetryCount() + 1);
            sms.setStatus(NotificationStatus.QUEUED.name());
            log.info("SMS {} retry attempt {}", sms.getId(), sms.getRetryCount());
        }
        notificationRepository.save(sms);
    }
}
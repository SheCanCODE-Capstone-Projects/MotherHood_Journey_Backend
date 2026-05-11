
package com.motherhood.journey.notification.service;

import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.notification.entity.SmsNotification;
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
}
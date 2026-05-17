package com.motherhood.journey.notification.service;

import com.motherhood.journey.child.entity.VaccinationRecord;
import com.motherhood.journey.identity.entity.User;
import com.motherhood.journey.notification.entity.SmsNotification;
import com.motherhood.journey.notification.enums.NotificationType;

public interface NotificationService {
    void enqueue(VaccinationRecord record);  // vaccination reminders
    void enqueueRaw(User recipient, String message,
                    NotificationType type);     // appointment reminders
    void retry(SmsNotification sms);
}
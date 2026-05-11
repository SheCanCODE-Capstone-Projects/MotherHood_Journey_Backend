
package com.motherhood.journey.notification.service;

import com.motherhood.journey.child.entity.VaccinationRecord;

public interface NotificationService {
    void enqueue(VaccinationRecord record);
}
package com.motherhood.journey.notification.repository;

import com.motherhood.journey.notification.entity.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<SmsNotification, UUID> {
}

package com.motherhood.journey.notification.repository;

import com.motherhood.journey.notification.entity.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<SmsNotification, UUID> {

    @Query("SELECT n FROM SmsNotification n WHERE n.status = 'QUEUED' AND n.scheduledAt <= :now ORDER BY n.scheduledAt ASC")
    List<SmsNotification> findDueNotifications(LocalDateTime now);

    List<SmsNotification> findByRecipientUser_IdOrderByCreatedAtDesc(UUID userId);

    long countByStatus(String status);
}

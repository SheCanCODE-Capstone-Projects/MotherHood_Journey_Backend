package com.motherhood.journey.notification.repository;

import com.motherhood.journey.notification.entity.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository
        extends JpaRepository<SmsNotification, UUID> {

    // Find all queued notifications that are due to be sent
    @Query("SELECT n FROM SmsNotification n " +
            "WHERE n.status = 'QUEUED' " +
            "AND n.scheduledAt <= :now " +
            "ORDER BY n.scheduledAt ASC")
    List<SmsNotification> findDueNotifications(LocalDateTime now);

    // Find notification by Africa's Talking message ID

    Optional<SmsNotification> findByAtMessageId(String atMessageId);

    // Find all notifications for a specific user
    List<SmsNotification> findByRecipientUser_IdOrderByCreatedAtDesc(
            UUID userId);

    // Count notifications by status
    long countByStatus(String status);
}
package com.motherhood.journey.notification.entity;

import com.motherhood.journey.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sms_notifications", indexes = {
        @Index(name = "idx_sms_user", columnList = "recipient_user_id"),
        @Index(name = "idx_sms_status", columnList = "status"),
        @Index(name = "idx_sms_scheduled", columnList = "scheduled_at"),
        @Index(name = "idx_sms_type", columnList = "notification_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsNotification {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipientUser;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "message_body", nullable = false, length = 32)
    private String messageBody;

    @Column(name = "notification_type", nullable = false, length = 32)
    private String notificationType;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "QUEUED";

    @Column(name = "at_message_id", length = 64)
    private String atMessageId;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
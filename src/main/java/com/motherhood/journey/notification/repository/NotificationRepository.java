package com.motherhood.journey.notification.repository;

import com.motherhood.journey.notification.entity.SmsNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<SmsNotification, UUID> {

	List<SmsNotification> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
			String status,
			LocalDateTime now
	);

	Optional<SmsNotification> findByAtMessageId(String atMessageId);
}

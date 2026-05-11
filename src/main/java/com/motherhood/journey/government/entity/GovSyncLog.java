package com.motherhood.journey.government.entity;

import com.motherhood.journey.government.enums.SyncStatus;
import com.motherhood.journey.government.enums.TargetSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "gov_sync_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovSyncLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, unique = true)
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_system", nullable = false)
	private TargetSystem targetSystem;

	@Column(name = "sync_type", nullable = false)
	private String syncType;

	@Column(name = "reference_no")
	private String referenceNo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", columnDefinition = "jsonb")
	private Map<String, Object> payload;

	@Column(name = "payload_hash")
	private String payloadHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private SyncStatus status = SyncStatus.PENDING;

	@Column(name = "retry_count", nullable = false)
	@Builder.Default
	private int retryCount = 0;

	@Column(name = "next_retry_at")
	private OffsetDateTime nextRetryAt;

	@Column(name = "last_error", columnDefinition = "TEXT")
	private String lastError;

	@Column(name = "dead_letter", nullable = false)
	@Builder.Default
	private boolean deadLetter = false;

	@Column(name = "created_at", nullable = false, updatable = false)
	@Builder.Default
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "updated_at", nullable = false)
	@Builder.Default
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}
}

package com.motherhood.journey.government.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_request_docs", indexes = {
        @Index(name = "idx_srd_request", columnList = "request_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestDoc {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest request;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "uploaded_at", updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
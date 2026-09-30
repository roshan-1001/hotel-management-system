package com.roshan.hotel.domain;

import com.roshan.hotel.enums.IdempotencyOperation;
import com.roshan.hotel.enums.IdempotencyStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_operation_key",
                        columnNames = {"operation_type","idempotency_key"}
                )
        }
)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private IdempotencyOperation operationType;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(
            IdempotencyOperation operationType,
            String idempotencyKey,
            String requestHash,
            Instant createdAt) {

        this.operationType = operationType;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = createdAt;
        this.status = IdempotencyStatus.IN_PROGRESS;
    }

    public void complete(Long resourceId) {
        this.resourceId = resourceId;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Long getResourceId() {
        return resourceId;

    }

    public IdempotencyOperation getOperationType() {
        return operationType;
    }
}
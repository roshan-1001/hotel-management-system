package com.roshan.hotel.repository;

import com.roshan.hotel.domain.IdempotencyRecord;
import com.roshan.hotel.enums.IdempotencyOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(
            IdempotencyOperation operationType,
            String idempotencyKey
    );

    @Modifying
    @Query(value = """
    INSERT INTO idempotency_records
    (operation_type, idempotency_key, request_hash, status, created_at)
    VALUES
        (:operationType, :key, :requestHash, 'IN_PROGRESS', :createdAt)
    ON CONFLICT (operation_type, idempotency_key) DO NOTHING
    """, nativeQuery = true)
    int claim(
            @Param("operationType") IdempotencyOperation operationType,
            @Param("key") String key,
            @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt
    );
}
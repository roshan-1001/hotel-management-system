package com.roshan.hotel.service;

import com.roshan.hotel.domain.IdempotencyRecord;
import com.roshan.hotel.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyRecordRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public boolean claim(String idempotencyKey, String requestHash) {

        int inserted = repository.claim(idempotencyKey, requestHash, clock.instant());
        return inserted == 1;
    }

    public IdempotencyRecord get(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Idempotency record not found: " + idempotencyKey
                        )
                );
    }

    public void complete(
            String idempotencyKey,
            Long resourceId) {

        IdempotencyRecord record = get(idempotencyKey);
        record.complete(resourceId);
    }
}

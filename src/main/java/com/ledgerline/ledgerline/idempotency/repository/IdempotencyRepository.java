package com.ledgerline.ledgerline.idempotency.repository;

import com.ledgerline.ledgerline.idempotency.entity.IdempotencyRecord;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository
        extends JpaRepository<IdempotencyRecord, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO idempotency_records
        (id, user_id, idempotency_key, request_hash,
         status, created_at, expires_at)
        VALUES
        (:id, :userId, :key, :hash,
         'PROCESSING', :createdAt, :expiresAt)
        ON CONFLICT (user_id, idempotency_key) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("key") String key,
            @Param("hash") String hash,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKey(
            UUID userId,
            String idempotencyKey
    );
}
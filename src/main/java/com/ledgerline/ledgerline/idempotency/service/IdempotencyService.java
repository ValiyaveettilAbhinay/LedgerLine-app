package com.ledgerline.ledgerline.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ledgerline.ledgerline.idempotency.entity.IdempotencyRecord;
import com.ledgerline.ledgerline.idempotency.entity.IdempotencyStatus;
import com.ledgerline.ledgerline.idempotency.repository.IdempotencyRepository;
import com.ledgerline.ledgerline.user.entity.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(
            IdempotencyRepository idempotencyRepository,
            ObjectMapper objectMapper
    ) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> T execute(
            User user,
            String idempotencyKey,
            String requestHash,
            Class<T> responseType,
            Supplier<T> operation
    ) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank() ||
                idempotencyKey.length() > 128) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // Attempt to claim the key atomically.
        int inserted = idempotencyRepository.insertIfAbsent(
                UUID.randomUUID(),
                user.getId(),
                idempotencyKey,
                requestHash,
                now,
                now.plusHours(24)
        );

        if (inserted == 0) {

            IdempotencyRecord existing =
                    idempotencyRepository
                            .findByUserIdAndIdempotencyKey(
                                    user.getId(),
                                    idempotencyKey
                            )
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Idempotency record not found"
                                    )
                            );

            // Same key cannot represent different requests.
            if (!existing.getRequestHash().equals(requestHash)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key reused with different request"
                );
            }

            // Return the original response.
            if (existing.getStatus() == IdempotencyStatus.COMPLETED) {

                try {
                    return objectMapper.readValue(
                            existing.getResponseBody(),
                            responseType
                    );

                } catch (JsonProcessingException e) {

                    throw new IllegalStateException(
                            "Unable to restore previous response", e
                    );
                }
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request is already being processed or cannot be retried"
            );
        }

        // Execute the actual financial operation.
        T response = operation.get();

        try {

            String responseBody =
                    objectMapper.writeValueAsString(response);

            IdempotencyRecord record =
                    idempotencyRepository
                            .findByUserIdAndIdempotencyKey(
                                    user.getId(),
                                    idempotencyKey
                            )
                            .orElseThrow();

            record.setStatus(IdempotencyStatus.COMPLETED);
            record.setResponseBody(responseBody);
            record.setResponseStatus(HttpStatus.CREATED.value());

            idempotencyRepository.save(record);

        } catch (JsonProcessingException e) {

            throw new IllegalStateException(
                    "Unable to save transaction response", e
            );
        }

        return response;
    }
}
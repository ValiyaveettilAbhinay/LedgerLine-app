package com.ledgerline.ledgerline.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class RequestHashService {

    private final ObjectMapper objectMapper;

    public RequestHashService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateHash(String operation, Object request) {

        try {
            // Include operation type to distinguish different API operations.
            String payload = objectMapper.writeValueAsString(
                    new HashPayload(operation, request)
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (JsonProcessingException | NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "Unable to generate request hash", e
            );
        }
    }

    private record HashPayload(
            String operation,
            Object request
    ) {}
}
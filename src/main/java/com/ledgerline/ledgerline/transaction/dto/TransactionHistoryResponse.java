package com.ledgerline.ledgerline.transaction.dto;

import com.ledgerline.ledgerline.transaction.entity.TransactionType;
import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionHistoryResponse(
        UUID transactionId,
        TransactionType type,
        TransactionStatus status,
        Long amountMinorUnits,
        String currency,
        String description,
        LocalDateTime createdAt
) {}
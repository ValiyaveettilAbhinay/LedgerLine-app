package com.ledgerline.ledgerline.admin.dto;

import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;
import com.ledgerline.ledgerline.transaction.entity.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminTransactionResponse(
        UUID transactionId,
        UUID initiatedByUserId,
        String initiatedByEmail,
        TransactionType type,
        TransactionStatus status,
        Long amountMinorUnits,
        String currency,
        String description,
        LocalDateTime createdAt
) {}
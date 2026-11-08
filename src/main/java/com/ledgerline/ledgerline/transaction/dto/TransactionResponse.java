package com.ledgerline.ledgerline.transaction.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(

        UUID transactionId,
        String transactionType,
        String transactionStatus,
        Long amountMinorUnits,
        String currency,
        String description,
        LocalDateTime createdAt

) {}
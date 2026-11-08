package com.ledgerline.ledgerline.transaction.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record WithdrawalResponse(
        UUID transactionId,
        UUID accountId,
        String accountNumber,
        Long amountMinorUnits,
        Long newBalanceMinorUnits,
        String currency,
        String status,
        LocalDateTime createdAt
) {}
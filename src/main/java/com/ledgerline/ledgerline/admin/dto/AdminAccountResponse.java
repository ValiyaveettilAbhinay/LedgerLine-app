package com.ledgerline.ledgerline.admin.dto;

import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.account.entity.AccountType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAccountResponse(
        UUID id,
        String accountNumber,
        Long balanceMinorUnits,
        String currency,
        AccountStatus status,
        AccountType accountType,
        UUID userId,
        LocalDateTime createdAt
) {}
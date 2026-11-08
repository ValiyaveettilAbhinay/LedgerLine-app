package com.ledgerline.ledgerline.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record WithdrawalRequest(

        @NotNull
        UUID accountId,

        @NotNull
        @Positive
        Long amountMinorUnits,

        @Size(max = 255)
        String description

) {}

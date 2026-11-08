package com.ledgerline.ledgerline.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TransferRequest(

        @NotNull(message = "Source account ID is required")
        UUID sourceAccountId,

        @NotBlank(message = "Destination account number is required")
        String destinationAccountNumber,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        Long amountMinorUnits,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description

) {}
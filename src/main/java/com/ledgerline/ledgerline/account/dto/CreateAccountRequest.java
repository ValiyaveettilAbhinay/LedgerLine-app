package com.ledgerline.ledgerline.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(

        @NotBlank(message = "Currency is required")
        @Pattern(
                regexp = "INR",
                message = "Only INR currency is supported"
        )
        String currency

) {}
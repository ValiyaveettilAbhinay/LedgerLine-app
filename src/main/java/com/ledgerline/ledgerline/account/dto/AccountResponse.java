package com.ledgerline.ledgerline.account.dto;

import com.ledgerline.ledgerline.account.entity.Account;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(

        UUID id,
        String accountNumber,
        Long balanceMinorUnits,
        String currency,
        String status,
        LocalDateTime createdAt

) {

    public static AccountResponse from(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalanceMinorUnits(),
                account.getCurrency(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}

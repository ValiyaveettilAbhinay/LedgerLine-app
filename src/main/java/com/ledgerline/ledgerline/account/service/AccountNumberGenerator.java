package com.ledgerline.ledgerline.account.service;

import com.ledgerline.ledgerline.account.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private final AccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generate() {

        for (int attempt = 0; attempt < 10; attempt++) {

            // Generate a 12-digit account number
            long number = 1_000_000_000_00L
                    + (long) (secureRandom.nextDouble() * 9_000_000_000_00L);

            String accountNumber = String.valueOf(number);

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique account number"
        );
    }
}

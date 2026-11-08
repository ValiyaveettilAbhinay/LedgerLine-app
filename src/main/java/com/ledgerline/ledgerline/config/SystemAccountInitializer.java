package com.ledgerline.ledgerline.config;

import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.account.entity.AccountType;
import com.ledgerline.ledgerline.account.repository.AccountRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SystemAccountInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    public SystemAccountInitializer(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        createClearingAccount("INR");

    }

    private void createClearingAccount(String currency) {

        boolean exists =
                accountRepository.existsByAccountTypeAndCurrency(
                        AccountType.CLEARING,
                        currency
                );

        if (exists) {
            System.out.println(
                    "Clearing account already exists for " + currency
            );
            return;
        }

        Account clearingAccount = new Account();

        clearingAccount.setUser(null);
        clearingAccount.setAccountNumber("SYS-" + currency + "-CLEARING");
        clearingAccount.setAccountType(AccountType.CLEARING);
        clearingAccount.setBalanceMinorUnits(0L);
        clearingAccount.setCurrency(currency);
        clearingAccount.setStatus(AccountStatus.ACTIVE);

        accountRepository.save(clearingAccount);

        System.out.println(
                "System clearing account created for " + currency
        );
    }
}
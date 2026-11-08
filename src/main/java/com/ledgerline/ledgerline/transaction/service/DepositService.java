package com.ledgerline.ledgerline.transaction.service;

import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.account.entity.AccountType;
import com.ledgerline.ledgerline.account.repository.AccountRepository;

import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;
import com.ledgerline.ledgerline.audit.entity.AuditAction;
import com.ledgerline.ledgerline.audit.service.AuditService;

import com.ledgerline.ledgerline.ledger.entity.EntryType;
import com.ledgerline.ledgerline.ledger.entity.LedgerEntry;
import com.ledgerline.ledgerline.ledger.repository.LedgerEntryRepository;

import com.ledgerline.ledgerline.transaction.dto.DepositRequest;
import com.ledgerline.ledgerline.transaction.dto.DepositResponse;

import com.ledgerline.ledgerline.transaction.entity.Transaction;
import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;
import com.ledgerline.ledgerline.transaction.entity.TransactionType;
import com.ledgerline.ledgerline.transaction.repository.TransactionRepository;

import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DepositService {

    private final AuditService auditService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public DepositService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            AuditService auditService
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DepositResponse deposit(
            String email,
            DepositRequest request
    ) {

        // 1. Find authenticated user

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // 2. Find customer account reference

        Account customerReference = accountRepository
                .findByIdAndUserId(request.accountId(), user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found")
                );

        // 3. Find system clearing account

        Account clearingReference = accountRepository
                .findByAccountTypeAndCurrency(
                        AccountType.CLEARING,
                        customerReference.getCurrency()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "System clearing account not found"
                        )
                );

        // 4. Lock both accounts

        List<Account> lockedAccounts =
                accountRepository.findAllByIdInForUpdate(
                        List.of(
                                customerReference.getId(),
                                clearingReference.getId()
                        )
                );

        if (lockedAccounts.size() != 2) {
            throw new ResourceNotFoundException(
                    "Customer or clearing account not found"
            );
        }

        Map<UUID, Account> accountMap = lockedAccounts.stream()
                .collect(Collectors.toMap(
                        Account::getId,
                        Function.identity()
                ));

        Account customerAccount =
                accountMap.get(customerReference.getId());

        Account clearingAccount =
                accountMap.get(clearingReference.getId());

        // 5. Validate account types and ownership

        if (customerAccount.getAccountType() != AccountType.CUSTOMER
                || customerAccount.getUser() == null
                || !customerAccount.getUser().getId().equals(user.getId())) {

            throw new ResourceNotFoundException("Account not found");
        }

        if (clearingAccount.getAccountType() != AccountType.CLEARING) {
            throw new IllegalStateException(
                    "Invalid system clearing account"
            );
        }

        // 6. Validate account statuses

        if (customerAccount.getStatus() != AccountStatus.ACTIVE
                || clearingAccount.getStatus() != AccountStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Both accounts must be active"
            );
        }

        // 7. Validate amount

        long amount = request.amountMinorUnits();

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be positive"
            );
        }

        // 8. Calculate balances safely

        long customerNewBalance;
        long clearingNewBalance;

        try {
            customerNewBalance = Math.addExact(
                    customerAccount.getBalanceMinorUnits(),
                    amount
            );

            clearingNewBalance = Math.addExact(
                    clearingAccount.getBalanceMinorUnits(),
                    amount
            );

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Deposit balance limit exceeded"
            );
        }

        // 9. Create transaction

        Transaction transaction = new Transaction();

        transaction.setInitiatedBy(user);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmountMinorUnits(amount);
        transaction.setCurrency(customerAccount.getCurrency());
        transaction.setDescription(request.description());

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        // 10. Create clearing account debit entry

        LedgerEntry clearingDebit = new LedgerEntry();

        clearingDebit.setTransaction(savedTransaction);
        clearingDebit.setAccount(clearingAccount);
        clearingDebit.setEntryType(EntryType.DEBIT);
        clearingDebit.setAmountMinorUnits(amount);
        clearingDebit.setCurrency(clearingAccount.getCurrency());

        // 11. Create customer account credit entry

        LedgerEntry customerCredit = new LedgerEntry();

        customerCredit.setTransaction(savedTransaction);
        customerCredit.setAccount(customerAccount);
        customerCredit.setEntryType(EntryType.CREDIT);
        customerCredit.setAmountMinorUnits(amount);
        customerCredit.setCurrency(customerAccount.getCurrency());

        ledgerEntryRepository.saveAll(
                List.of(clearingDebit, customerCredit)
        );

        // 12. Update balances

        customerAccount.setBalanceMinorUnits(customerNewBalance);

        clearingAccount.setBalanceMinorUnits(clearingNewBalance);

        accountRepository.saveAll(
                List.of(customerAccount, clearingAccount)
        );

        // 13. Return response

        auditService.log(
                user,
                AuditAction.DEPOSIT_COMPLETED,
                "FINANCIAL_TRANSACTION",
                transaction.getId(),
                "Deposit completed successfully. Amount: "
                        + transaction.getAmountMinorUnits()
                        + " "
                        + transaction.getCurrency()
        );

        return new DepositResponse(
                savedTransaction.getId(),
                customerAccount.getId(),
                customerAccount.getAccountNumber(),
                amount,
                customerNewBalance,
                customerAccount.getCurrency(),
                savedTransaction.getStatus().name(),
                savedTransaction.getCreatedAt()
        );
    }
}
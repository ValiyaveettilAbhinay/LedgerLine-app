package com.ledgerline.ledgerline.transaction.service;

import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.entity.AccountType;
import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.account.repository.AccountRepository;
import com.ledgerline.ledgerline.audit.entity.AuditAction;
import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;
import com.ledgerline.ledgerline.ledger.entity.EntryType;
import com.ledgerline.ledgerline.ledger.entity.LedgerEntry;
import com.ledgerline.ledgerline.ledger.repository.LedgerEntryRepository;
import com.ledgerline.ledgerline.transaction.dto.WithdrawalRequest;
import com.ledgerline.ledgerline.transaction.dto.WithdrawalResponse;
import com.ledgerline.ledgerline.transaction.entity.Transaction;
import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;
import com.ledgerline.ledgerline.transaction.entity.TransactionType;
import com.ledgerline.ledgerline.transaction.repository.TransactionRepository;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;
import com.ledgerline.ledgerline.audit.entity.AuditLog;
import com.ledgerline.ledgerline.audit.entity.AuditAction;
import com.ledgerline.ledgerline.audit.service.AuditService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AuditService auditService;

    public WithdrawalService(
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
        this.auditService= auditService;
    }

    @Transactional
    public WithdrawalResponse withdraw(
            String email,
            WithdrawalRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Account customerAccount = accountRepository
                .findByIdAndUserId(request.accountId(), user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found"));

        Account clearingReference = accountRepository
                .findByAccountTypeAndCurrency(
                        AccountType.CLEARING,
                        customerAccount.getCurrency()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Clearing account not found"));

        // Lock both accounts before modifying balances.
        List<Account> lockedAccounts =
                accountRepository.findAllByIdInForUpdate(
                        List.of(customerAccount.getId(), clearingReference.getId())
                );

        Account lockedCustomer = lockedAccounts.stream()
                .filter(a -> a.getId().equals(customerAccount.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found"));

        Account lockedClearing = lockedAccounts.stream()
                .filter(a -> a.getId().equals(clearingReference.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Clearing account not found"));

        if (lockedCustomer.getStatus() != AccountStatus.ACTIVE
                || lockedClearing.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }

        if (lockedCustomer.getAccountType() != AccountType.CUSTOMER
                || lockedClearing.getAccountType() != AccountType.CLEARING) {
            throw new IllegalArgumentException("Invalid account configuration");
        }

        Long amount = request.amountMinorUnits();

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        if (lockedCustomer.getBalanceMinorUnits() < amount) {
            throw new IllegalArgumentException("Insufficient customer balance");
        }

        if (lockedClearing.getBalanceMinorUnits() < amount) {
            throw new IllegalArgumentException("Insufficient clearing balance");
        }

        long newCustomerBalance =
                Math.subtractExact(lockedCustomer.getBalanceMinorUnits(), amount);

        long newClearingBalance =
                Math.subtractExact(lockedClearing.getBalanceMinorUnits(), amount);

        // Create transaction record.
        Transaction transaction = new Transaction();

        transaction.setInitiatedBy(user);
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setTransactionStatus(TransactionStatus.COMPLETED);
        transaction.setAmountMinorUnits(amount);
        transaction.setCurrency(lockedCustomer.getCurrency());
        transaction.setDescription(request.description());

        transaction = transactionRepository.save(transaction);

        // Customer account: DEBIT.
        LedgerEntry customerDebit = new LedgerEntry();

        customerDebit.setTransaction(transaction);
        customerDebit.setAccount(lockedCustomer);
        customerDebit.setEntryType(EntryType.DEBIT);
        customerDebit.setAmountMinorUnits(amount);
        customerDebit.setCurrency(lockedCustomer.getCurrency());

        // Clearing account: CREDIT.
        LedgerEntry clearingCredit = new LedgerEntry();

        clearingCredit.setTransaction(transaction);
        clearingCredit.setAccount(lockedClearing);
        clearingCredit.setEntryType(EntryType.CREDIT);
        clearingCredit.setAmountMinorUnits(amount);
        clearingCredit.setCurrency(lockedClearing.getCurrency());

        ledgerEntryRepository.saveAll(
                List.of(customerDebit, clearingCredit)
        );

        // Update balances.
        lockedCustomer.setBalanceMinorUnits(newCustomerBalance);
        lockedClearing.setBalanceMinorUnits(newClearingBalance);

        accountRepository.saveAll(
                List.of(lockedCustomer, lockedClearing)
        );

        auditService.log(
                user,
                AuditAction.WITHDRAWAL_COMPLETED,
                "FINANCIAL_TRANSACTION",
                transaction.getId(),
                "Withdrawal completed successfully. Amount: "
                        + transaction.getAmountMinorUnits()
                        + " "
                        + transaction.getCurrency()
        );

        return new WithdrawalResponse(
                transaction.getId(),
                lockedCustomer.getId(),
                lockedCustomer.getAccountNumber(),
                amount,
                newCustomerBalance,
                lockedCustomer.getCurrency(),
                transaction.getTransactionStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
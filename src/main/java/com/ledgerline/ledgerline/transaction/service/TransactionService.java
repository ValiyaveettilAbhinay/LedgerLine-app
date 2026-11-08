package com.ledgerline.ledgerline.transaction.service;

import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.repository.AccountRepository;
import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.ledger.entity.EntryType;
import com.ledgerline.ledgerline.transaction.dto.TransactionHistoryResponse;
import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;
import com.ledgerline.ledgerline.transaction.entity.TransactionType;
import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;
import com.ledgerline.ledgerline.ledger.entity.LedgerEntry;
import com.ledgerline.ledgerline.ledger.repository.LedgerEntryRepository;
import com.ledgerline.ledgerline.transaction.dto.TransactionResponse;
import com.ledgerline.ledgerline.transaction.dto.TransferRequest;
import com.ledgerline.ledgerline.transaction.entity.Transaction;
import com.ledgerline.ledgerline.transaction.repository.TransactionRepository;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.ledgerline.ledgerline.audit.entity.AuditAction;
import com.ledgerline.ledgerline.audit.service.AuditService;
import com.ledgerline.ledgerline.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
@Service
public class TransactionService {

    private final AuditService auditService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    @Transactional(readOnly = true)
    public Page<TransactionHistoryResponse> getTransactionHistory(
            String email,
            TransactionType type,
            TransactionStatus status,
            LocalDate from,
            LocalDate to,
            Pageable pageable
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        UUID userId = user.getId();

        LocalDateTime fromDate = from != null
                ? from.atStartOfDay()
                : null;

        LocalDateTime toDate = to != null
                ? to.plusDays(1).atStartOfDay()
                : null;

        Specification<Transaction> specification =
                (root, query, criteriaBuilder) -> {

                    query.distinct(true);

                    // Transactions initiated by this user
                    Predicate initiatedByUser = criteriaBuilder.equal(
                            root.get("initiatedBy").get("id"),
                            userId
                    );

                    // Transactions where this user owns an involved account
                    Subquery<Integer> subquery =
                            query.subquery(Integer.class);

                    var ledgerEntry = subquery.from(LedgerEntry.class);

                    subquery.select(criteriaBuilder.literal(1));

                    subquery.where(
                            criteriaBuilder.equal(
                                    ledgerEntry.get("transaction"),
                                    root
                            ),
                            criteriaBuilder.equal(
                                    ledgerEntry.get("account")
                                            .get("user")
                                            .get("id"),
                                    userId
                            )
                    );

                    Predicate involvedInTransaction =
                            criteriaBuilder.exists(subquery);

                    // User must be involved in the transaction
                    Predicate userAccess = criteriaBuilder.or(
                            initiatedByUser,
                            involvedInTransaction
                    );

                    // Optional filters
                    Predicate filters = criteriaBuilder.conjunction();

                    if (type != null) {
                        filters = criteriaBuilder.and(
                                filters,
                                criteriaBuilder.equal(
                                        root.get("type"),
                                        type
                                )
                        );
                    }

                    if (status != null) {
                        filters = criteriaBuilder.and(
                                filters,
                                criteriaBuilder.equal(
                                        root.get("status"),
                                        status
                                )
                        );
                    }

                    if (fromDate != null) {
                        filters = criteriaBuilder.and(
                                filters,
                                criteriaBuilder.greaterThanOrEqualTo(
                                        root.get("createdAt"),
                                        fromDate
                                )
                        );
                    }

                    if (toDate != null) {
                        filters = criteriaBuilder.and(
                                filters,
                                criteriaBuilder.lessThan(
                                        root.get("createdAt"),
                                        toDate
                                )
                        );
                    }

                    query.orderBy(
                            criteriaBuilder.desc(
                                    root.get("createdAt")
                            )
                    );

                    return criteriaBuilder.and(
                            userAccess,
                            filters
                    );
                };

        return transactionRepository
                .findAll(specification, pageable)
                .map(transaction -> new TransactionHistoryResponse(
                        transaction.getId(),
                        transaction.getType(),
                        transaction.getStatus(),
                        transaction.getAmountMinorUnits(),
                        transaction.getCurrency(),
                        transaction.getDescription(),
                        transaction.getCreatedAt()
                ));
    }

    public TransactionService(
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


    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyTransactions(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return transactionRepository
                .findAllByInitiatedByIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(transaction -> new TransactionResponse(
                        transaction.getId(),
                        transaction.getType().name(),
                        transaction.getStatus().name(),
                        transaction.getAmountMinorUnits(),
                        transaction.getCurrency(),
                        transaction.getDescription(),
                        transaction.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public TransactionResponse transfer(
            String email,
            TransferRequest request
    ) {

        // 1. Find authenticated user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // 2. Find destination account
        Account destinationReference = accountRepository
                .findByAccountNumber(request.destinationAccountNumber())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Destination account not found"
                        ));

        UUID sourceId = request.sourceAccountId();
        UUID destinationId = destinationReference.getId();

        // 3. Prevent transferring to the same account
        if (sourceId.equals(destinationId)) {
            throw new IllegalArgumentException(
                    "Source and destination accounts cannot be the same"
            );
        }

        // 4. Lock both accounts
        List<Account> lockedAccounts =
                accountRepository.findAllByIdInForUpdate(
                        List.of(sourceId, destinationId)
                );

        if (lockedAccounts.size() != 2) {
            throw new ResourceNotFoundException(
                    "One or both accounts were not found"
            );
        }

        Map<UUID, Account> accountMap = lockedAccounts.stream()
                .collect(Collectors.toMap(
                        Account::getId,
                        Function.identity()
                ));

        Account source = accountMap.get(sourceId);
        Account destination = accountMap.get(destinationId);

        // 5. Validate source ownership
        if (!source.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Source account not found");
        }

        // 6. Validate account statuses
        if (source.getStatus() != AccountStatus.ACTIVE ||
                destination.getStatus() != AccountStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Both accounts must be active"
            );
        }

        // 7. Validate currency
        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new IllegalArgumentException(
                    "Account currencies must match"
            );
        }

        // 8. Validate amount
        long amount = request.amountMinorUnits();

        if (amount <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be positive"
            );
        }

        // 9. Validate sufficient balance
        if (source.getBalanceMinorUnits() < amount) {
            throw new IllegalArgumentException(
                    "Insufficient account balance"
            );
        }

        // 10. Calculate new balances safely
        long sourceBalance =
                source.getBalanceMinorUnits() - amount;

        long destinationBalance;

        try {
            destinationBalance = Math.addExact(
                    destination.getBalanceMinorUnits(),
                    amount
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Destination balance limit exceeded"
            );
        }

        // 11. Create financial transaction
        Transaction transaction = new Transaction();

        transaction.setInitiatedBy(user);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setAmountMinorUnits(amount);
        transaction.setCurrency(source.getCurrency());
        transaction.setDescription(request.description());

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        // 12. Create debit ledger entry
        LedgerEntry debit = new LedgerEntry();

        debit.setTransaction(savedTransaction);
        debit.setAccount(source);
        debit.setEntryType(EntryType.DEBIT);
        debit.setAmountMinorUnits(amount);
        debit.setCurrency(source.getCurrency());

        // 13. Create credit ledger entry
        LedgerEntry credit = new LedgerEntry();

        credit.setTransaction(savedTransaction);
        credit.setAccount(destination);
        credit.setEntryType(EntryType.CREDIT);
        credit.setAmountMinorUnits(amount);
        credit.setCurrency(destination.getCurrency());

        ledgerEntryRepository.saveAll(List.of(debit, credit));

        // 14. Update account balances
        source.setBalanceMinorUnits(sourceBalance);
        destination.setBalanceMinorUnits(destinationBalance);

        accountRepository.saveAll(List.of(source, destination));

        auditService.log(
                user,
                AuditAction.TRANSFER_COMPLETED,
                "FINANCIAL_TRANSACTION",
                transaction.getId(),
                "Transfer completed successfully. Amount: "
                        + transaction.getAmountMinorUnits()
                        + " "
                        + transaction.getCurrency()
        );


        // 15. Return transaction response
        return new TransactionResponse(
                savedTransaction.getId(),
                savedTransaction.getType().name(),
                savedTransaction.getStatus().name(),
                savedTransaction.getAmountMinorUnits(),
                savedTransaction.getCurrency(),
                savedTransaction.getDescription(),
                savedTransaction.getCreatedAt()
        );
    }
}
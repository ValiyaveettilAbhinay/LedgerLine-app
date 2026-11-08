package com.ledgerline.ledgerline.admin.service;

import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.account.repository.AccountRepository;
import com.ledgerline.ledgerline.admin.dto.AdminAccountResponse;
import com.ledgerline.ledgerline.admin.dto.AdminUserResponse;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.transaction.repository.TransactionRepository;
import com.ledgerline.ledgerline.admin.dto.AdminTransactionResponse;
import com.ledgerline.ledgerline.transaction.entity.Transaction;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import com.ledgerline.ledgerline.account.entity.AccountType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ledgerline.ledgerline.audit.entity.AuditAction;
import com.ledgerline.ledgerline.audit.service.AuditService;
import com.ledgerline.ledgerline.common.enums.UserRole;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    public AdminService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }


    @Transactional(readOnly = true)
    public Page<AdminTransactionResponse> getAllTransactions(
            Pageable pageable
    ) {

        return transactionRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(transaction -> new AdminTransactionResponse(
                        transaction.getId(),
                        transaction.getInitiatedBy().getId(),
                        transaction.getInitiatedBy().getEmail(),
                        transaction.getType(),
                        transaction.getStatus(),
                        transaction.getAmountMinorUnits(),
                        transaction.getCurrency(),
                        transaction.getDescription(),
                        transaction.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {

        return userRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(user -> new AdminUserResponse(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt()
                ));
    }

    @Transactional(readOnly = true)
    public Page<AdminAccountResponse> getAllAccounts(Pageable pageable) {

        return accountRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAccountResponse);
    }

    @Transactional
    public AdminAccountResponse updateAccountStatus(
            UUID accountId,
            AccountStatus newStatus,
            String adminEmail
    ) {

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                NOT_FOUND, "Administrator not found"
                        ));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException(
                    "Administrator privileges required"
            );
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                NOT_FOUND, "Account not found"
                        ));

        if (account.getAccountType() != AccountType.CUSTOMER) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Clearing accounts cannot be modified"
            );
        }

        if (newStatus == AccountStatus.CLOSED) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Account closure is not supported"
            );
        }

        AccountStatus previousStatus = account.getStatus();

        if (previousStatus == newStatus) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Account already has this status"
            );
        }

        account.setStatus(newStatus);

        accountRepository.save(account);

        AuditAction action = switch (newStatus) {
            case FROZEN -> AuditAction.ACCOUNT_FROZEN;
            case ACTIVE -> AuditAction.ACCOUNT_REACTIVATED;
            default -> throw new ResponseStatusException(
                    BAD_REQUEST, "Unsupported account status"
            );
        };

        auditService.log(
                admin,
                action,
                "ACCOUNT",
                account.getId(),
                "Account status changed from "
                        + previousStatus
                        + " to "
                        + newStatus
        );

        return toAccountResponse(account);
    }

    private AdminAccountResponse toAccountResponse(Account account) {

        return new AdminAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalanceMinorUnits(),
                account.getCurrency(),
                account.getStatus(),
                account.getAccountType(),
                account.getUser() == null ? null : account.getUser().getId(),
                account.getCreatedAt()
        );
    }
}
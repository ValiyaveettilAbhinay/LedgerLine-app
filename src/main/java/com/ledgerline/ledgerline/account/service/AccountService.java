package com.ledgerline.ledgerline.account.service;

import com.ledgerline.ledgerline.account.dto.AccountResponse;
import com.ledgerline.ledgerline.account.dto.CreateAccountRequest;
import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.account.repository.AccountRepository;
import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            AccountNumberGenerator accountNumberGenerator
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @Transactional
    public AccountResponse freezeAccount(UUID accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found"));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "Closed accounts cannot be frozen"
            );
        }

        account.setStatus(AccountStatus.FROZEN);

        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(UUID accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found"));

        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new IllegalArgumentException(
                    "Only frozen accounts can be unfrozen"
            );
        }

        account.setStatus(AccountStatus.ACTIVE);

        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse createAccount(
            String email,
            CreateAccountRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Account account = new Account();

        account.setUser(user);
        account.setAccountNumber(accountNumberGenerator.generate());
        account.setBalanceMinorUnits(0L);
        account.setCurrency(request.currency());

        Account savedAccount = accountRepository.save(account);

        return AccountResponse.from(savedAccount);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return accountRepository.findAllByUserId(user.getId())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(
            UUID accountId,
            String email
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Account account = accountRepository
                .findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found"));
        return AccountResponse.from(account);
    }
}
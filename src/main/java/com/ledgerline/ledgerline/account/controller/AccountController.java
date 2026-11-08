package com.ledgerline.ledgerline.account.controller;

import com.ledgerline.ledgerline.account.dto.AccountResponse;
import com.ledgerline.ledgerline.account.dto.CreateAccountRequest;
import com.ledgerline.ledgerline.account.service.AccountService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Create a new account
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication
    ) {

        String email = authentication.getName();

        AccountResponse response =
                accountService.createAccount(email, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all accounts belonging to the logged-in user
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                accountService.getMyAccounts(email)
        );
    }

    // Get a specific account
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable UUID accountId,
            Authentication authentication
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                accountService.getAccountById(accountId, email)
        );
    }

    @PatchMapping("/{accountId}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> freezeAccount(
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(
                accountService.freezeAccount(accountId)
        );
    }

    @PatchMapping("/{accountId}/unfreeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> unfreezeAccount(
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(
                accountService.unfreezeAccount(accountId)
        );
    }
}
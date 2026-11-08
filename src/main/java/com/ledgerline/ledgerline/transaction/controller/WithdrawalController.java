package com.ledgerline.ledgerline.transaction.controller;

import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;
import com.ledgerline.ledgerline.idempotency.service.IdempotencyService;
import com.ledgerline.ledgerline.idempotency.service.RequestHashService;
import com.ledgerline.ledgerline.transaction.dto.WithdrawalRequest;
import com.ledgerline.ledgerline.transaction.dto.WithdrawalResponse;
import com.ledgerline.ledgerline.transaction.service.WithdrawalService;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final IdempotencyService idempotencyService;
    private final RequestHashService requestHashService;
    private final UserRepository userRepository;

    public WithdrawalController(
            WithdrawalService withdrawalService,
            IdempotencyService idempotencyService,
            RequestHashService requestHashService,
            UserRepository userRepository
    ) {
        this.withdrawalService = withdrawalService;
        this.idempotencyService = idempotencyService;
        this.requestHashService = requestHashService;
        this.userRepository = userRepository;
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawalResponse> withdraw(

            @RequestHeader("Idempotency-Key") String idempotencyKey,

            @Valid @RequestBody WithdrawalRequest request,

            Authentication authentication
    ) {

        String email = authentication.getName();

        // 1. Find authenticated user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // 2. Generate request hash
        String requestHash = requestHashService.generateHash(
                "WITHDRAWAL",
                request
        );

        // 3. Execute through idempotency service
        WithdrawalResponse response = idempotencyService.execute(
                user,
                idempotencyKey,
                requestHash,
                WithdrawalResponse.class,
                () -> withdrawalService.withdraw(email, request)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
package com.ledgerline.ledgerline.transaction.controller;

import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;
import com.ledgerline.ledgerline.idempotency.service.IdempotencyService;
import com.ledgerline.ledgerline.idempotency.service.RequestHashService;
import com.ledgerline.ledgerline.transaction.dto.DepositRequest;
import com.ledgerline.ledgerline.transaction.dto.DepositResponse;
import com.ledgerline.ledgerline.transaction.service.DepositService;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class DepositController {

    private final DepositService depositService;
    private final IdempotencyService idempotencyService;
    private final RequestHashService requestHashService;
    private final UserRepository userRepository;

    public DepositController(
            DepositService depositService,
            IdempotencyService idempotencyService,
            RequestHashService requestHashService,
            UserRepository userRepository
    ) {
        this.depositService = depositService;
        this.idempotencyService = idempotencyService;
        this.requestHashService = requestHashService;
        this.userRepository = userRepository;
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(

            @RequestHeader("Idempotency-Key") String idempotencyKey,

            @Valid @RequestBody DepositRequest request,

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
                "DEPOSIT",
                request
        );

        // 3. Execute deposit through idempotency service
        DepositResponse response = idempotencyService.execute(
                user,
                idempotencyKey,
                requestHash,
                DepositResponse.class,
                () -> depositService.deposit(email, request)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
package com.ledgerline.ledgerline.transaction.controller;

import com.ledgerline.ledgerline.idempotency.service.IdempotencyService;
import com.ledgerline.ledgerline.idempotency.service.RequestHashService;
import com.ledgerline.ledgerline.transaction.dto.TransactionHistoryResponse;
import com.ledgerline.ledgerline.transaction.dto.TransactionResponse;
import com.ledgerline.ledgerline.transaction.dto.TransferRequest;
import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;
import com.ledgerline.ledgerline.transaction.entity.TransactionType;
import com.ledgerline.ledgerline.transaction.service.TransactionService;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;
import com.ledgerline.ledgerline.common.exception.ResourceNotFoundException;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;
    private final RequestHashService requestHashService;
    private final UserRepository userRepository;

    @GetMapping("/history")
    public Page<TransactionHistoryResponse> getTransactionHistory(
            Authentication authentication,

            @RequestParam(required = false)
            TransactionType type,

            @RequestParam(required = false)
            TransactionStatus status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            Pageable pageable
    ) {
        return transactionService.getTransactionHistory(
                authentication.getName(),
                type,
                status,
                from,
                to,
                pageable
        );
    }

    public TransactionController(
            TransactionService transactionService,
            IdempotencyService idempotencyService,
            RequestHashService requestHashService,
            UserRepository userRepository
    ) {
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
        this.requestHashService = requestHashService;
        this.userRepository = userRepository;
    }

    // Transfer money between accounts with idempotency
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(

            @RequestHeader("Idempotency-Key") String idempotencyKey,

            @Valid @RequestBody TransferRequest request,

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
                "TRANSFER",
                request
        );

        // 3. Execute through idempotency service
        TransactionResponse response = idempotencyService.execute(
                user,
                idempotencyKey,
                requestHash,
                TransactionResponse.class,
                () -> transactionService.transfer(email, request)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }



    // Get transaction history
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getMyTransactions(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                transactionService.getMyTransactions(email)
        );
    }
}
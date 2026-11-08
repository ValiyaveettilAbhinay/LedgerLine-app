package com.ledgerline.ledgerline.audit.controller;

import com.ledgerline.ledgerline.audit.dto.AuditLogResponse;
import com.ledgerline.ledgerline.audit.service.AuditService;
import com.ledgerline.ledgerline.user.entity.User;
import com.ledgerline.ledgerline.user.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AuditController(
            AuditService auditService,
            UserRepository userRepository
    ) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Page<AuditLogResponse> getMyAuditLogs(
            Authentication authentication,
            Pageable pageable
    ) {
        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return auditService.getMyAuditLogs(user.getId(), pageable);
    }
}
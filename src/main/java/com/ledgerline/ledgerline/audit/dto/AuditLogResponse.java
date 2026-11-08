package com.ledgerline.ledgerline.audit.dto;

import com.ledgerline.ledgerline.audit.entity.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        AuditAction action,
        String entityType,
        UUID entityId,
        String details,
        LocalDateTime createdAt
) {}
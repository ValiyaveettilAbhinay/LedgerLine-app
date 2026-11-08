package com.ledgerline.ledgerline.audit.service;

import com.ledgerline.ledgerline.audit.dto.AuditLogResponse;
import com.ledgerline.ledgerline.audit.entity.AuditAction;
import com.ledgerline.ledgerline.audit.entity.AuditLog;
import com.ledgerline.ledgerline.audit.repository.AuditLogRepository;
import com.ledgerline.ledgerline.user.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(
            User user,
            AuditAction action,
            String entityType,
            UUID entityId,
            String details
    ) {

        AuditLog auditLog = new AuditLog(
                user,
                action,
                entityType,
                entityId,
                details
        );

        auditLogRepository.save(auditLog);
    }
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getMyAuditLogs(
            UUID userId,
            Pageable pageable
    ) {
        return auditLogRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getDetails(),
                        log.getCreatedAt()
                ));
    }
}
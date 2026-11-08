package com.ledgerline.ledgerline.audit.repository;

import com.ledgerline.ledgerline.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByUserIdOrderByCreatedAtDesc(
            UUID userId,
            Pageable pageable
    );

    Page<AuditLog> findAllByEntityIdOrderByCreatedAtDesc(
            UUID entityId,
            Pageable pageable
    );
}
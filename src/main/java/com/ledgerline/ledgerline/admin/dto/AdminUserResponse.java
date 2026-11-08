package com.ledgerline.ledgerline.admin.dto;

import com.ledgerline.ledgerline.common.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {}
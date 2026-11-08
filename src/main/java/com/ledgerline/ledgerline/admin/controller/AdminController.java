package com.ledgerline.ledgerline.admin.controller;

import com.ledgerline.ledgerline.account.entity.AccountStatus;
import com.ledgerline.ledgerline.admin.dto.AdminAccountResponse;
import com.ledgerline.ledgerline.admin.dto.AdminUserResponse;
import com.ledgerline.ledgerline.admin.service.AdminService;
import org.springframework.security.core.Authentication;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    @GetMapping("/accounts")
    public Page<AdminAccountResponse> getAllAccounts(Pageable pageable) {
        return adminService.getAllAccounts(pageable);
    }

    @PatchMapping("/accounts/{accountId}/status")
    public AdminAccountResponse updateAccountStatus(
            @PathVariable UUID accountId,
            @RequestParam AccountStatus status,
            Authentication authentication
    ) {
        return adminService.updateAccountStatus(
                accountId,
                status,
                authentication.getName()
        );
    }
}
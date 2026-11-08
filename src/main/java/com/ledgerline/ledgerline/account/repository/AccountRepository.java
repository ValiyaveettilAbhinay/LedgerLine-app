package com.ledgerline.ledgerline.account.repository;

import com.ledgerline.ledgerline.account.entity.Account;
import com.ledgerline.ledgerline.account.entity.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByUserId(UUID userId);

    Optional<Account> findByIdAndUserId(UUID accountId, UUID userId);

    boolean existsByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT a FROM Account a
    WHERE a.id IN :accountIds
    ORDER BY a.id
""")
    List<Account> findAllByIdInForUpdate(
            @Param("accountIds") Collection<UUID> accountIds
    );
    boolean existsByAccountTypeAndCurrency(
            AccountType accountType,
            String currency
    );
    Optional<Account> findByAccountTypeAndCurrency(
            AccountType accountType,
            String currency
    );

    Page<Account> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
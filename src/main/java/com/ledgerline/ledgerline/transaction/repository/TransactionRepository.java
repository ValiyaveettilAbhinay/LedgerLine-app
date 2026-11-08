//package com.ledgerline.ledgerline.transaction.repository;
//
//import com.ledgerline.ledgerline.transaction.entity.Transaction;
//
//import com.ledgerline.ledgerline.transaction.entity.TransactionStatus;
//import com.ledgerline.ledgerline.transaction.entity.TransactionType;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
//    List<Transaction> findAllByInitiatedByIdOrderByCreatedAtDesc(
//            UUID userId
//    );
//
//    @Query("""
//    SELECT DISTINCT t
//    FROM Transaction t
//    WHERE (
//        t.initiatedBy.id = :userId
//        OR EXISTS (
//            SELECT le.id
//            FROM LedgerEntry le
//            WHERE le.transaction = t
//            AND le.account.user.id = :userId
//        )
//    )
//    AND (:type IS NULL OR t.type = :type)
//    AND (:status IS NULL OR t.status = :status)
//    AND (:fromDate IS NULL OR t.createdAt >= :fromDate)
//    AND (:toDate IS NULL OR t.createdAt < :toDate)
//    ORDER BY t.createdAt DESC
//""")
//    Page<Transaction> findUserTransactionHistory(
//            @Param("userId") UUID userId,
//            @Param("type") TransactionType type,
//            @Param("status") TransactionStatus status,
//            @Param("fromDate") LocalDateTime fromDate,
//            @Param("toDate") LocalDateTime toDate,
//            Pageable pageable
//    );
//
//    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
//
//    Optional<Object> findAll(Specification<Transaction> specification, Pageable pageable);
//}
//
//

package com.ledgerline.ledgerline.transaction.repository;

import com.ledgerline.ledgerline.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends
        JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findAllByInitiatedByIdOrderByCreatedAtDesc(
            UUID userId
    );

    Page<Transaction> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );
}
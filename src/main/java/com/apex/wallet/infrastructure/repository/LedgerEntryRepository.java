package com.apex.wallet.infrastructure.repository;

import com.apex.wallet.domain.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<LedgerEntry> findByTransactionId(Long transactionId);

    List<LedgerEntry> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = 'DEBIT'")
    BigDecimal sumTotalDebits();

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = 'CREDIT'")
    BigDecimal sumTotalCredits();
}

package com.apex.wallet.application.service;

import com.apex.wallet.domain.model.LedgerEntry;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LedgerAuditService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    public LedgerAuditService(LedgerEntryRepository ledgerEntryRepository,
                              AccountRepository accountRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> performAudit() {
        BigDecimal totalDebits = ledgerEntryRepository.sumTotalDebits();
        BigDecimal totalCredits = ledgerEntryRepository.sumTotalCredits();
        BigDecimal totalAccountBalances = accountRepository.sumTotalBalances();

        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;
        if (totalAccountBalances == null) totalAccountBalances = BigDecimal.ZERO;

        BigDecimal netLedgerEquity = totalCredits.subtract(totalDebits);
        BigDecimal discrepancy = totalAccountBalances.subtract(netLedgerEquity).abs();
        boolean isSound = discrepancy.compareTo(new BigDecimal("0.001")) < 0;

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("totalDebits", totalDebits);
        audit.put("totalCredits", totalCredits);
        audit.put("netLedgerEquity", netLedgerEquity);
        audit.put("totalAccountBalances", totalAccountBalances);
        audit.put("discrepancy", discrepancy);
        audit.put("ledgerIntegrityStatus", isSound ? "VERIFIED_SOUND" : "DISCREPANCY_DETECTED");
        audit.put("auditTimestamp", java.time.LocalDateTime.now());
        audit.put("totalEntriesCount", ledgerEntryRepository.count());

        return audit;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesByAccount(Long accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getAllEntries() {
        return ledgerEntryRepository.findAllByOrderByCreatedAtDesc();
    }
}

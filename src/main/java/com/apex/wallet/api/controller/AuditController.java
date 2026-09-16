package com.apex.wallet.api.controller;

import com.apex.wallet.application.service.LedgerAuditService;
import com.apex.wallet.domain.model.LedgerEntry;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.infrastructure.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final LedgerAuditService ledgerAuditService;
    private final TransactionRepository transactionRepository;

    public AuditController(LedgerAuditService ledgerAuditService, TransactionRepository transactionRepository) {
        this.ledgerAuditService = ledgerAuditService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping({"/summary", "/balance"})
    public ResponseEntity<Map<String, Object>> getAuditSummary() {
        return ResponseEntity.ok(ledgerAuditService.performAudit());
    }

    @GetMapping({"/ledger-entries", "/entries"})
    public ResponseEntity<List<LedgerEntry>> getAllLedgerEntries() {
        return ResponseEntity.ok(ledgerAuditService.getAllEntries());
    }

    @GetMapping("/all-transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionRepository.findAllByOrderByCreatedAtDesc());
    }
}

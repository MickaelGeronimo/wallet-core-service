package com.apex.wallet.api.controller;

import com.apex.wallet.application.service.LedgerAuditService;
import com.apex.wallet.domain.model.LedgerEntry;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.domain.outbox.OutboxEvent;
import com.apex.wallet.domain.outbox.OutboxStatus;
import com.apex.wallet.infrastructure.outbox.OutboxEventRepository;
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
    private final OutboxEventRepository outboxEventRepository;

    public AuditController(LedgerAuditService ledgerAuditService,
                           TransactionRepository transactionRepository,
                           OutboxEventRepository outboxEventRepository) {
        this.ledgerAuditService = ledgerAuditService;
        this.transactionRepository = transactionRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @GetMapping({"/summary", "/balance"})
    public ResponseEntity<Map<String, Object>> getAuditSummary() {
        return ResponseEntity.ok(ledgerAuditService.performAudit());
    }

    @GetMapping({"/ledger-entries", "/entries"})
    public ResponseEntity<List<LedgerEntry>> getAllLedgerEntries(
            @org.springframework.web.bind.annotation.RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null && limit > 0) {
            var page = ledgerAuditService.getAllEntries(org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 500)));
            return ResponseEntity.ok(page.getContent());
        }
        return ResponseEntity.ok(ledgerAuditService.getAllEntries());
    }

    @GetMapping("/all-transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @org.springframework.web.bind.annotation.RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null && limit > 0) {
            var page = transactionRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 500)));
            return ResponseEntity.ok(page.getContent());
        }
        return ResponseEntity.ok(transactionRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/outbox")
    public ResponseEntity<List<OutboxEvent>> getAllOutboxEvents() {
        return ResponseEntity.ok(outboxEventRepository.findAll());
    }

    @GetMapping("/outbox/pending")
    public ResponseEntity<List<OutboxEvent>> getPendingOutboxEvents() {
        return ResponseEntity.ok(outboxEventRepository.findByStatus(OutboxStatus.PENDING));
    }

    @GetMapping("/outbox/dead-letter")
    public ResponseEntity<List<OutboxEvent>> getDeadLetterOutboxEvents() {
        return ResponseEntity.ok(outboxEventRepository.findByStatus(OutboxStatus.DEAD_LETTER));
    }
}

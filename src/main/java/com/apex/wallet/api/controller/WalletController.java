package com.apex.wallet.api.controller;

import com.apex.wallet.api.dto.DepositRequest;
import com.apex.wallet.application.service.AccountService;
import com.apex.wallet.application.service.LedgerAuditService;
import com.apex.wallet.application.service.TransferService;
import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.LedgerEntry;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.infrastructure.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final AccountService accountService;
    private final TransferService transferService;
    private final TransactionRepository transactionRepository;
    private final LedgerAuditService ledgerAuditService;
    private final com.apex.wallet.application.resilience.BalanceCacheService balanceCacheService;

    public WalletController(AccountService accountService,
                            TransferService transferService,
                            TransactionRepository transactionRepository,
                            LedgerAuditService ledgerAuditService,
                            com.apex.wallet.application.resilience.BalanceCacheService balanceCacheService) {
        this.accountService = accountService;
        this.transferService = transferService;
        this.transactionRepository = transactionRepository;
        this.ledgerAuditService = ledgerAuditService;
        this.balanceCacheService = balanceCacheService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyAccount(@AuthenticationPrincipal Account currentAccount) {
        try {
            Account fresh = accountService.getAccountById(currentAccount.getId());
            return ResponseEntity.ok(fresh);
        } catch (Exception e) {
            return balanceCacheService.getBalance(currentAccount.getId())
                    .<ResponseEntity<?>>map(cached -> ResponseEntity.ok()
                            .header("X-Degraded-Mode", "true")
                            .body(Map.of(
                                    "id", currentAccount.getId(),
                                    "holderName", cached.holderName(),
                                    "accountNumber", cached.accountNumber(),
                                    "balance", cached.balance(),
                                    "email", currentAccount.getEmail(),
                                    "pixKey", currentAccount.getPixKey() != null ? currentAccount.getPixKey() : "",
                                    "degraded", true,
                                    "cachedAt", cached.lastUpdated().toString()
                            )))
                    .orElseGet(() -> ResponseEntity.ok(currentAccount));
        }
    }

    @GetMapping("/cached-balance")
    public ResponseEntity<?> getCachedBalance(@AuthenticationPrincipal Account currentAccount) {
        return balanceCacheService.getBalance(currentAccount.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<Map<String, Object>>> getAvailableAccounts(@AuthenticationPrincipal Account currentAccount) {
        List<Map<String, Object>> list = accountService.getAllAccounts().stream()
                .filter(acc -> !acc.getId().equals(currentAccount.getId()))
                .map(acc -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", acc.getId());
                    map.put("accountNumber", acc.getAccountNumber());
                    map.put("holderName", acc.getHolderName());
                    map.put("pixKey", acc.getPixKey());
                    map.put("email", acc.getEmail());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/pix-lookup")
    public ResponseEntity<?> lookupPix(@RequestParam("key") String pixKey) {
        Optional<Account> targetOpt = accountService.lookupPixKey(pixKey);
        if (targetOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Chave PIX não encontrada."));
        }
        Account target = targetOpt.get();
        return ResponseEntity.ok(Map.of(
                "accountId", target.getId(),
                "holderName", target.getHolderName(),
                "accountNumber", target.getAccountNumber(),
                "pixKey", target.getPixKey()
        ));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getMyTransactions(
            @AuthenticationPrincipal Account currentAccount,
            @RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null && limit > 0) {
            var page = transactionRepository.findByAccountId(currentAccount.getId(), org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 100)));
            return ResponseEntity.ok(page.getContent());
        }
        List<Transaction> transactions = transactionRepository.findByAccountId(currentAccount.getId());
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/ledger")
    public ResponseEntity<List<LedgerEntry>> getMyLedger(
            @AuthenticationPrincipal Account currentAccount,
            @RequestParam(name = "limit", required = false) Integer limit) {
        if (limit != null && limit > 0) {
            var page = ledgerAuditService.getEntriesByAccount(currentAccount.getId(), org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 100)));
            return ResponseEntity.ok(page.getContent());
        }
        List<LedgerEntry> entries = ledgerAuditService.getEntriesByAccount(currentAccount.getId());
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(
            @AuthenticationPrincipal Account currentAccount,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {

        String key = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : "DEP-" + UUID.randomUUID();
        Transaction tx = transferService.executeDeposit(key, currentAccount.getId(), request.getAmount(), request.getDescription());
        return ResponseEntity.ok(tx);
    }
}

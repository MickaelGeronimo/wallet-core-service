package com.apex.wallet.application.service;

import com.apex.wallet.application.resilience.BalanceCacheService;
import com.apex.wallet.application.resilience.BufferedTransferQueueService;
import com.apex.wallet.application.resilience.ChaosManager;
import com.apex.wallet.application.resilience.QueuedTransferItem;
import com.apex.wallet.application.risk.RiskAssessmentService;
import com.apex.wallet.domain.model.*;
import com.apex.wallet.domain.outbox.OutboxEvent;
import com.apex.wallet.domain.risk.RiskContext;
import com.apex.wallet.domain.risk.RiskRejectedException;
import com.apex.wallet.infrastructure.outbox.OutboxEventRepository;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.repository.LedgerEntryRepository;
import com.apex.wallet.infrastructure.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final RiskAssessmentService riskAssessmentService;
    private final OutboxEventRepository outboxEventRepository;
    private final BalanceCacheService balanceCacheService;
    private final BufferedTransferQueueService queueService;
    private final ChaosManager chaosManager;
    private final EntityManager entityManager;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           RiskAssessmentService riskAssessmentService,
                           OutboxEventRepository outboxEventRepository,
                           BalanceCacheService balanceCacheService,
                           BufferedTransferQueueService queueService,
                           ChaosManager chaosManager,
                           EntityManager entityManager) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.riskAssessmentService = riskAssessmentService;
        this.outboxEventRepository = outboxEventRepository;
        this.balanceCacheService = balanceCacheService;
        this.queueService = queueService;
        this.chaosManager = chaosManager;
        this.entityManager = entityManager;
    }

    /**
     * Executes an atomic, idempotent, double-entry financial transfer between two accounts.
     * Incorporates:
     * - Distributed Idempotency verification
     * - Antifraud / AML Risk Pipeline (Sanctions, Velocity Burst, Limits)
     * - Deadlock-free Pessimistic Locking on database rows
     * - Double-entry ledger postings (Debit + Credit = 0)
     * - Transactional Outbox event generation in the exact same ACID unit of work
     * - Resilience4j Circuit Breaker with Graceful Degradation Fallback
     */
    @CircuitBreaker(name = "transferService", fallbackMethod = "fallbackExecuteTransfer")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction executeTransfer(String idempotencyKey, Long sourceAccountId, String targetPixKey, BigDecimal amount, String description) {
        return executeSettlementDirect(idempotencyKey, sourceAccountId, targetPixKey, amount, description);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction executeSettlementDirect(String idempotencyKey, Long sourceAccountId, String targetPixKey, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transferência deve ser positivo.");
        }

        // Apply chaos delay or forced failure if active (for simulation)
        chaosManager.applyChaosIfActive();

        // 1. Idempotency Check: Return existing transaction if key already exists
        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            log.info("Chave de idempotência já processada: {}. Retornando transação existente.", idempotencyKey);
            return existingTx.get();
        }

        // 2. Antifraud / AML Risk Screening Pipeline
        Account sourceAccountMeta = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de origem não encontrada: " + sourceAccountId));

        long recentTxCount = transactionRepository.countBySourceAccountIdAndCreatedAtAfter(
                sourceAccountId, LocalDateTime.now().minus(60, ChronoUnit.SECONDS)
        );

        RiskContext riskContext = new RiskContext(
                sourceAccountMeta,
                targetPixKey,
                amount,
                "127.0.0.1",
                recentTxCount,
                BigDecimal.ZERO
        );
        riskAssessmentService.assessTransaction(riskContext);

        // 3. Resolve target account by Pix key
        Account targetAccountMeta = accountRepository.findByPixKey(targetPixKey)
                .orElseThrow(() -> new IllegalArgumentException("Chave PIX destinatária não encontrada: " + targetPixKey));

        Long targetAccountId = targetAccountMeta.getId();
        if (sourceAccountId.equals(targetAccountId)) {
            throw new IllegalArgumentException("A conta de origem e destino não podem ser a mesma.");
        }

        // 4. Deadlock Prevention Strategy: Always acquire locks in ascending order of Account ID
        Long firstId = Math.min(sourceAccountId, targetAccountId);
        Long secondId = Math.max(sourceAccountId, targetAccountId);

        Account firstLocked = accountRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + firstId));
        Account secondLocked = accountRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + secondId));

        entityManager.refresh(firstLocked);
        entityManager.refresh(secondLocked);

        Account sourceAccount = sourceAccountId.equals(firstId) ? firstLocked : secondLocked;
        Account targetAccount = targetAccountId.equals(firstId) ? firstLocked : secondLocked;

        // 5. Validate Balance (Protected by exclusive row lock!)
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(String.format("Saldo insuficiente. Saldo disponível: R$ %.2f, Valor requerido: R$ %.2f",
                    sourceAccount.getBalance(), amount));
        }

        // 6. Update Balances
        BigDecimal sourceNewBalance = sourceAccount.getBalance().subtract(amount);
        BigDecimal targetNewBalance = targetAccount.getBalance().add(amount);

        sourceAccount.setBalance(sourceNewBalance);
        targetAccount.setBalance(targetNewBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // Update in-memory cache for fast degraded lookups
        balanceCacheService.updateBalance(sourceAccountId, sourceNewBalance);
        balanceCacheService.updateBalance(targetAccountId, targetNewBalance);

        // 7. Create Transaction record
        Transaction transaction = new Transaction(
                idempotencyKey,
                sourceAccountId,
                targetAccountId,
                amount,
                TransactionType.PIX_TRANSFER,
                TransactionStatus.COMPLETED,
                description != null ? description : "Transferência PIX para " + targetAccount.getHolderName()
        );
        Transaction savedTx = transactionRepository.save(transaction);

        // 8. Double-Entry Bookkeeping (Partidas Dobradas)
        LedgerEntry debitEntry = new LedgerEntry(
                savedTx.getId(),
                sourceAccountId,
                LedgerType.DEBIT,
                amount,
                sourceNewBalance,
                "Débito PIX para " + targetAccount.getHolderName() + " (" + targetAccount.getPixKey() + ")"
        );
        ledgerEntryRepository.save(debitEntry);

        LedgerEntry creditEntry = new LedgerEntry(
                savedTx.getId(),
                targetAccountId,
                LedgerType.CREDIT,
                amount,
                targetNewBalance,
                "Crédito PIX recebido de " + sourceAccount.getHolderName()
        );
        ledgerEntryRepository.save(creditEntry);

        // 9. Transactional Outbox Pattern
        String outboxPayload = String.format(
                "{\"txId\":%d,\"amount\":\"%s\",\"debtor\":\"%s\",\"creditor\":\"%s\",\"type\":\"PIX_TRANSFER\"}",
                savedTx.getId(), amount.toPlainString(), sourceAccount.getHolderName(), targetAccount.getHolderName()
        );
        OutboxEvent outboxEvent = new OutboxEvent("TRANSFER", savedTx.getId().toString(), "PAYMENT_SETTLED", outboxPayload);
        outboxEventRepository.save(outboxEvent);

        log.info("Transferência realizada com sucesso: TxId={}, De={} Para={}, Valor=R$ {}",
                savedTx.getId(), sourceAccount.getHolderName(), targetAccount.getHolderName(), amount);

        return savedTx;
    }

    /**
     * Graceful Degradation Fallback:
     * When the database connection pool is exhausted or under extreme load,
     * this fallback absorbs the write into the in-memory buffer without dropping HTTP 500.
     * Note: Domain and Risk validations are never degraded and are propagated immediately.
     */
    public Transaction fallbackExecuteTransfer(String idempotencyKey, Long sourceAccountId, String targetPixKey, BigDecimal amount, String description, Throwable t) throws Throwable {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            if (root instanceof RiskRejectedException || root instanceof IllegalArgumentException || root instanceof IllegalStateException) {
                break;
            }
            root = root.getCause();
        }
        if (root instanceof RiskRejectedException || root instanceof IllegalArgumentException || root instanceof IllegalStateException) {
            throw root;
        }

        log.warn("DEGRAÇÃO GRACIOSA: Circuit Breaker interceptou transferência idempKey={}. Motivo: {}",
                idempotencyKey, t.getMessage());

        QueuedTransferItem queuedItem = new QueuedTransferItem(
                idempotencyKey,
                sourceAccountId,
                targetPixKey,
                amount,
                description != null ? description : "Transferência PIX Degradada",
                Instant.now(),
                0
        );
        queueService.enqueue(queuedItem);

        // Return a queued transaction receipt with 202 Accepted status
        Transaction queuedReceipt = new Transaction(
                idempotencyKey,
                sourceAccountId,
                null,
                amount,
                TransactionType.PIX_TRANSFER,
                TransactionStatus.QUEUED_FOR_SETTLEMENT,
                "DEGRADAÇÃO GRACIOSA ATIVADA: Transação retida em buffer de alta velocidade. Liquidação automática em fila assíncrona."
        );

        return queuedReceipt;
    }

    /**
     * Executes an instant deposit / wallet recharge.
     */
    @Transactional
    public Transaction executeDeposit(String idempotencyKey, Long accountId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }

        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            return existingTx.get();
        }

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada: " + accountId));
        entityManager.refresh(account);

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        balanceCacheService.updateBalance(accountId, newBalance);

        Transaction transaction = new Transaction(
                idempotencyKey,
                null,
                accountId,
                amount,
                TransactionType.DEPOSIT,
                TransactionStatus.COMPLETED,
                description != null ? description : "Recarga de Carteira"
        );
        Transaction savedTx = transactionRepository.save(transaction);

        LedgerEntry creditEntry = new LedgerEntry(
                savedTx.getId(),
                accountId,
                LedgerType.CREDIT,
                amount,
                newBalance,
                "Depósito / Recarga de Saldo"
        );
        ledgerEntryRepository.save(creditEntry);

        OutboxEvent outboxEvent = new OutboxEvent(
                "DEPOSIT", savedTx.getId().toString(), "WALLET_DEPOSITED",
                String.format("{\"txId\":%d,\"amount\":\"%s\",\"account\":\"%s\"}", savedTx.getId(), amount.toPlainString(), account.getHolderName())
        );
        outboxEventRepository.save(outboxEvent);

        return savedTx;
    }
}

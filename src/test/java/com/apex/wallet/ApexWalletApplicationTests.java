package com.apex.wallet;

import com.apex.wallet.application.service.TransferService;
import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.domain.outbox.OutboxEvent;
import com.apex.wallet.domain.risk.RiskRejectedException;
import com.apex.wallet.infrastructure.outbox.OutboxEventRepository;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class ApexWalletApplicationTests {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private com.apex.wallet.infrastructure.repository.TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private com.apex.wallet.application.resilience.ChaosManager chaosManager;

    @Autowired
    private com.apex.wallet.application.resilience.BufferedTransferQueueService queueService;

    @Autowired
    private com.apex.wallet.infrastructure.resilience.QueueDrainWorker queueDrainWorker;

    @Autowired(required = false)
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private io.swagger.v3.oas.models.OpenAPI openAPI;

    @Autowired
    private com.apex.wallet.application.service.AuthService authService;

    @Autowired
    private com.apex.wallet.application.service.LedgerAuditService ledgerAuditService;

    @Autowired
    private com.apex.wallet.api.controller.AuditController auditController;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        chaosManager.reset();
        queueService.clear();
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.find("transferService").ifPresent(io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        chaosManager.reset();
        queueService.clear();
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.find("transferService").ifPresent(io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);
        }
    }

    @Test
    void contextLoads() {
        Assertions.assertTrue(accountRepository.count() > 0, "Personas should be initialized in database");
    }

    @Test
    @DisplayName("OpenAPI / Swagger: OpenApi specification must configure interactive Bearer JWT Authorization")
    void testOpenApiConfiguration() {
        Assertions.assertNotNull(openAPI, "OpenAPI bean must be present in Spring Context");
        Assertions.assertNotNull(openAPI.getInfo());
        Assertions.assertEquals("1.0.0", openAPI.getInfo().getVersion());
        Assertions.assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("BearerAuth"));
    }

    @Test
    @DisplayName("Idempotency: Re-submitting the exact same transaction key must return original transaction without deducting twice")
    void testIdempotencyProtection() {
        Account sender = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();
        Account recipient = accountRepository.findByEmail("beatriz@wallet.local").orElseThrow();

        BigDecimal initialBalance = sender.getBalance();
        String idempotencyKey = "TEST-IDEMPOTENCY-" + UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("100.00");

        // First attempt
        Transaction tx1 = transferService.executeTransfer(idempotencyKey, sender.getId(), recipient.getPixKey(), transferAmount, "Teste Idempotencia 1");
        Assertions.assertNotNull(tx1.getId());

        // Second attempt with exact same key
        Transaction tx2 = transferService.executeTransfer(idempotencyKey, sender.getId(), recipient.getPixKey(), transferAmount, "Teste Idempotencia 2 (Retry)");
        Assertions.assertEquals(tx1.getId(), tx2.getId(), "Must return identical transaction ID");

        // Verify balance was deducted ONLY ONCE
        Account senderAfter = accountRepository.findById(sender.getId()).orElseThrow();
        Assertions.assertEquals(initialBalance.subtract(transferAmount), senderAfter.getBalance(), "Balance must be deducted only once despite duplicate request");
    }

    @Test
    @DisplayName("Concurrency: Simultaneous parallel transfers must NEVER cause double-spending or negative balance")
    void testConcurrentTransfersNoDoubleSpending() throws InterruptedException {
        Account sender = accountRepository.findByEmail("carlos@wallet.local").orElseThrow();
        Account recipient = accountRepository.findByEmail("beatriz@wallet.local").orElseThrow();

        int threadCount = 5;
        BigDecimal transferAmount = new BigDecimal("100.00");
        BigDecimal initialBalance = sender.getBalance();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    String key = "CONC-TEST-" + UUID.randomUUID();
                    transferService.executeTransfer(key, sender.getId(), recipient.getPixKey(), transferAmount, "Concorrência Paralela");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignored or expected if balance was drained
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertTrue(finished);

        // Under Graceful Degradation, any concurrent lock contention is absorbed into the queue.
        // We drain the queue to ensure all transactions achieve eventual consistency.
        queueDrainWorker.drainAll();

        Account senderAfter = accountRepository.findById(sender.getId()).orElseThrow();
        BigDecimal expectedDeduction = transferAmount.multiply(new BigDecimal(threadCount));
        Assertions.assertEquals(initialBalance.subtract(expectedDeduction), senderAfter.getBalance(), "Balance must perfectly reflect all executed transactions with zero race conditions");
    }

    @Test
    @DisplayName("Antifraud: Sanctioned recipient keys must be blocked immediately by the Risk Screening Chain")
    void testAntifraudSanctionsBlock() {
        Account sender = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();

        RiskRejectedException ex = Assertions.assertThrows(
                RiskRejectedException.class,
                () -> transferService.executeTransfer(
                        "SANCTION-KEY-" + UUID.randomUUID(),
                        sender.getId(),
                        "blocked@fraud.com",
                        new BigDecimal("150.00"),
                        "Transferência para conta sancionada"
                )
        );

        Assertions.assertTrue(ex.getMessage().contains("SANCTIONS_AND_BLACKLIST_RULE"), "Error message should identify Sanctions rule violation");
    }

    @Test
    @DisplayName("Antifraud: High value instant transfer exceeding R$ 100.000 must be rejected by regulatory threshold")
    void testAntifraudHighValueThreshold() {
        Account admin = accountRepository.findByEmail("admin@wallet.local").orElseThrow();
        Account recipient = accountRepository.findByEmail("beatriz@wallet.local").orElseThrow();

        RiskRejectedException ex = Assertions.assertThrows(
                RiskRejectedException.class,
                () -> transferService.executeTransfer(
                        "HIGH-VAL-" + UUID.randomUUID(),
                        admin.getId(),
                        recipient.getPixKey(),
                        new BigDecimal("150000.00"),
                        "Transferência acima do limite"
                )
        );

        Assertions.assertTrue(ex.getMessage().contains("HIGH_VALUE_THRESHOLD_RULE"), "Error message should identify High Value Threshold rule");
    }

    @Test
    @DisplayName("Transactional Outbox: A successful transaction must automatically register an Outbox Event for async message dispatch")
    void testTransactionalOutboxEventCreated() {
        Account sender = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();
        Account recipient = accountRepository.findByEmail("beatriz@wallet.local").orElseThrow();

        String key = "OUTBOX-TEST-" + UUID.randomUUID();
        Transaction tx = transferService.executeTransfer(key, sender.getId(), recipient.getPixKey(), new BigDecimal("50.00"), "Teste Outbox Event");

        List<OutboxEvent> events = outboxEventRepository.findAll();
        boolean hasMatchingEvent = events.stream()
                .anyMatch(e -> e.getAggregateId().equals(tx.getId().toString()) && e.getEventType().equals("PAYMENT_SETTLED"));

        Assertions.assertTrue(hasMatchingEvent, "Must find OutboxEvent corresponding to the executed transfer transaction");
    }

    @Test
    @DisplayName("Exception Handling: RiskRejectedException must be translated to HTTP 422 Unprocessable Entity with structured payload")
    void testRiskRejectedExceptionHandler() {
        var handler = new com.apex.wallet.api.exception.GlobalExceptionHandler();
        var riskResult = com.apex.wallet.domain.risk.RiskAssessmentResult.rejected(
                "SANCTIONS_AND_BLACKLIST_RULE",
                "Chave destinatária consta na lista restritiva",
                1.0
        );
        var ex = new RiskRejectedException(riskResult);
        var response = handler.handleRiskRejected(ex);

        Assertions.assertEquals(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("SANCTIONS_AND_BLACKLIST_RULE", response.getBody().get("rule"));
        Assertions.assertEquals("Chave destinatária consta na lista restritiva", response.getBody().get("reason"));
    }

    @Test
    @DisplayName("Exception Handling: DataIntegrityViolationException must be translated to HTTP 409 Conflict")
    void testDataIntegrityViolationExceptionHandler() {
        var handler = new com.apex.wallet.api.exception.GlobalExceptionHandler();
        var ex = new org.springframework.dao.DataIntegrityViolationException("Unique constraint violation: idx_tx_idempotency");
        var response = handler.handleDataIntegrityViolation(ex);

        Assertions.assertEquals(org.springframework.http.HttpStatus.CONFLICT, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(409, response.getBody().get("status"));
        Assertions.assertTrue(response.getBody().get("message").toString().contains("chave de idempotência duplicada"));
    }

    @Test
    @DisplayName("PIX Resolution: Pix keys with whitespace and uppercase letters must resolve deterministically")
    void testPixKeyCaseInsensitiveAndTrimmingTransfer() {
        Account sender = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();
        Account recipient = accountRepository.findByEmail("beatriz@wallet.local").orElseThrow();

        // Target pix key formatted with uppercase and surrounding whitespace
        String messyPixKey = "  BEATRIZ@PIX.COM  ";
        String key = "PIX-CASE-" + UUID.randomUUID();

        Transaction tx = transferService.executeTransfer(key, sender.getId(), messyPixKey, new BigDecimal("15.00"), "Teste Case Insensitive");

        Assertions.assertNotNull(tx);
        Assertions.assertEquals(recipient.getId(), tx.getTargetAccountId(), "Must correctly resolve target account ID despite uppercase/whitespace Pix key");
    }

    @Test
    @DisplayName("Security & OWASP: Account entity password hash must NEVER leak into JSON serialization")
    void testAccountPasswordExcludedFromJsonSerialization() throws Exception {
        Account user = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        String json = mapper.writeValueAsString(user);

        Assertions.assertFalse(json.contains("password"), "JSON output must NOT contain password field or hash");
        Assertions.assertTrue(json.contains("lucas@wallet.local"));
    }

    @Test
    @DisplayName("Exception Handling: RequestNotPermitted must be translated to HTTP 429 Too Many Requests")
    void testRateLimiterExceptionHandler() {
        var handler = new com.apex.wallet.api.exception.GlobalExceptionHandler();
        var rateLimiter = io.github.resilience4j.ratelimiter.RateLimiter.ofDefaults("testLimiter");
        var ex = io.github.resilience4j.ratelimiter.RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        var response = handler.handleRateLimiter(ex);

        Assertions.assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(429, response.getBody().get("status"));
        Assertions.assertTrue(response.getBody().get("message").toString().contains("Taxa máxima de requisições excedida"));
    }

    @Test
    @DisplayName("Security & OWASP A07: Bad credentials must throw BadCredentialsException and map to HTTP 401 without user enumeration")
    void testBadCredentialsReturnsUnauthorized() {
        // 1. Test non-existent email
        var exNotFound = Assertions.assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.authenticate("naoexiste@wallet.local", "qualquersenha")
        );

        // 2. Test existing user with wrong password
        var exWrongPass = Assertions.assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.authenticate("lucas@wallet.local", "senhaincorreta")
        );

        // 3. Messages must be identical to prevent user enumeration
        Assertions.assertEquals(exNotFound.getMessage(), exWrongPass.getMessage(), "Error message must be identical for non-existent and wrong-password accounts to prevent user enumeration");

        // 4. Exception handler must translate to HTTP 401 Unauthorized
        var handler = new com.apex.wallet.api.exception.GlobalExceptionHandler();
        var response = handler.handleBadCredentials(exNotFound);
        Assertions.assertEquals(org.springframework.http.HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Assertions.assertEquals(401, response.getBody().get("status"));
    }

    @Test
    @DisplayName("Ledger Reconciliation: Perform mathematical audit asserting sum(Balances) == TotalCredits - TotalDebits with zero discrepancy")
    void testLedgerMathematicalIntegrityReconciliation() {
        var audit = ledgerAuditService.performAudit();

        Assertions.assertNotNull(audit);
        Assertions.assertEquals("VERIFIED_SOUND", audit.get("ledgerIntegrityStatus"), "Ledger must be mathematically sound");
        Assertions.assertEquals(0, ((BigDecimal) audit.get("discrepancy")).compareTo(BigDecimal.ZERO), "Discrepancy must be 0.00");

        BigDecimal netLedgerEquity = (BigDecimal) audit.get("netLedgerEquity");
        BigDecimal totalAccountBalances = (BigDecimal) audit.get("totalAccountBalances");
        Assertions.assertEquals(netLedgerEquity, totalAccountBalances, "Net ledger equity must exactly equal total account balances");
    }

    @Test
    @DisplayName("Outbox Observability: Audit controller endpoints must expose Outbox events and statuses")
    void testOutboxAuditControllerEndpoints() {
        var allEvents = auditController.getAllOutboxEvents();
        Assertions.assertEquals(org.springframework.http.HttpStatus.OK, allEvents.getStatusCode());
        Assertions.assertNotNull(allEvents.getBody());

        var pendingEvents = auditController.getPendingOutboxEvents();
        Assertions.assertEquals(org.springframework.http.HttpStatus.OK, pendingEvents.getStatusCode());
        Assertions.assertNotNull(pendingEvents.getBody());

        var deadLetterEvents = auditController.getDeadLetterOutboxEvents();
        Assertions.assertEquals(org.springframework.http.HttpStatus.OK, deadLetterEvents.getStatusCode());
        Assertions.assertNotNull(deadLetterEvents.getBody());
    }

    @Test
    @DisplayName("Antifraud & AML: Bidirectional screening must immediately block transfer initiated by a sanctioned sender account")
    void testBidirectionalSanctionsBlocksSanctionedSender() {
        Account sanctionedSender = new Account("SANCT-101", "Sanctioned Person", "blocked@fraud.com", "pass", "sanct@pix.com", BigDecimal.ZERO, "ROLE_USER");
        Account savedSender = accountRepository.save(sanctionedSender);
        try {
            Account recipient = accountRepository.findByEmail("beatriz@wallet.local").orElseThrow();

            RiskRejectedException ex = Assertions.assertThrows(
                    RiskRejectedException.class,
                    () -> transferService.executeTransfer(
                            "SANCTION-SENDER-" + UUID.randomUUID(),
                            savedSender.getId(),
                            recipient.getPixKey(),
                            new BigDecimal("50.00"),
                            "Tentativa de evasão de sanções por remetente bloqueado"
                    )
            );

            Assertions.assertTrue(ex.getMessage().contains("SANCTIONS_AND_BLACKLIST_RULE"));
            Assertions.assertTrue(ex.getMessage().contains("Conta de origem"));
        } finally {
            accountRepository.delete(savedSender);
        }
    }

    @Test
    @DisplayName("Fail-Fast Validation: Blank or whitespace target Pix key must fail immediately before row locking")
    void testTransferWithBlankPixKeyFailsFast() {
        Account sender = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> transferService.executeTransfer(
                        "BLANK-PIX-" + UUID.randomUUID(),
                        sender.getId(),
                        "   ",
                        new BigDecimal("10.00"),
                        "Chave em branco"
                )
        );

        Assertions.assertTrue(ex.getMessage().contains("A chave PIX do destinatário é obrigatória"));
    }

    @Test
    @DisplayName("Performance & Pagination: Bounded history queries must return controlled page slices without memory exhaustion")
    void testPaginatedTransactionAndLedgerHistory() {
        Account sender = accountRepository.findByEmail("lucas@wallet.local").orElseThrow();

        var pagedTx = transactionRepository.findByAccountId(sender.getId(), org.springframework.data.domain.PageRequest.of(0, 2));
        Assertions.assertNotNull(pagedTx);
        Assertions.assertTrue(pagedTx.getNumberOfElements() <= 2);

        var pagedLedger = ledgerAuditService.getEntriesByAccount(sender.getId(), org.springframework.data.domain.PageRequest.of(0, 2));
        Assertions.assertNotNull(pagedLedger);
        Assertions.assertTrue(pagedLedger.getNumberOfElements() <= 2);
    }
}

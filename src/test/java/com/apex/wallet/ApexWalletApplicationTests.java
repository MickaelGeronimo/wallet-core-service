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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private com.apex.wallet.application.resilience.ChaosManager chaosManager;

    @Autowired
    private com.apex.wallet.application.resilience.BufferedTransferQueueService queueService;

    @Autowired
    private com.apex.wallet.infrastructure.resilience.QueueDrainWorker queueDrainWorker;

    @Autowired(required = false)
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

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
    @DisplayName("Idempotency: Re-submitting the exact same transaction key must return original transaction without deducting twice")
    void testIdempotencyProtection() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        BigDecimal initialBalance = deepak.getBalance();
        String idempotencyKey = "TEST-IDEMPOTENCY-" + UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("100.00");

        // First attempt
        Transaction tx1 = transferService.executeTransfer(idempotencyKey, deepak.getId(), marlon.getPixKey(), transferAmount, "Teste Idempotencia 1");
        Assertions.assertNotNull(tx1.getId());

        // Second attempt with exact same key
        Transaction tx2 = transferService.executeTransfer(idempotencyKey, deepak.getId(), marlon.getPixKey(), transferAmount, "Teste Idempotencia 2 (Retry)");
        Assertions.assertEquals(tx1.getId(), tx2.getId(), "Must return identical transaction ID");

        // Verify balance was deducted ONLY ONCE
        Account deepakAfter = accountRepository.findById(deepak.getId()).orElseThrow();
        Assertions.assertEquals(initialBalance.subtract(transferAmount), deepakAfter.getBalance(), "Balance must be deducted only once despite duplicate request");
    }

    @Test
    @DisplayName("Concurrency: Simultaneous parallel transfers must NEVER cause double-spending or negative balance")
    void testConcurrentTransfersNoDoubleSpending() throws InterruptedException {
        Account duBin = accountRepository.findByEmail("dubin@microsoft.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        int threadCount = 5;
        BigDecimal transferAmount = new BigDecimal("100.00");
        BigDecimal initialBalance = duBin.getBalance();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    String key = "CONC-TEST-" + UUID.randomUUID();
                    transferService.executeTransfer(key, duBin.getId(), marlon.getPixKey(), transferAmount, "Concorrência Paralela");
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

        Account duBinAfter = accountRepository.findById(duBin.getId()).orElseThrow();
        BigDecimal expectedDeduction = transferAmount.multiply(new BigDecimal(threadCount));
        Assertions.assertEquals(initialBalance.subtract(expectedDeduction), duBinAfter.getBalance(), "Balance must perfectly reflect all executed transactions with zero race conditions");
    }

    @Test
    @DisplayName("Antifraud: Sanctioned recipient keys must be blocked immediately by the Risk Screening Chain")
    void testAntifraudSanctionsBlock() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();

        RiskRejectedException ex = Assertions.assertThrows(
                RiskRejectedException.class,
                () -> transferService.executeTransfer(
                        "SANCTION-KEY-" + UUID.randomUUID(),
                        deepak.getId(),
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
        Account admin = accountRepository.findByEmail("admin@apex.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        RiskRejectedException ex = Assertions.assertThrows(
                RiskRejectedException.class,
                () -> transferService.executeTransfer(
                        "HIGH-VAL-" + UUID.randomUUID(),
                        admin.getId(),
                        marlon.getPixKey(),
                        new BigDecimal("150000.00"),
                        "Transferência acima do limite"
                )
        );

        Assertions.assertTrue(ex.getMessage().contains("HIGH_VALUE_THRESHOLD_RULE"), "Error message should identify High Value Threshold rule");
    }

    @Test
    @DisplayName("Transactional Outbox: A successful transaction must automatically register an Outbox Event for async message dispatch")
    void testTransactionalOutboxEventCreated() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        String key = "OUTBOX-TEST-" + UUID.randomUUID();
        Transaction tx = transferService.executeTransfer(key, deepak.getId(), marlon.getPixKey(), new BigDecimal("50.00"), "Teste Outbox Event");

        List<OutboxEvent> events = outboxEventRepository.findAll();
        boolean hasMatchingEvent = events.stream()
                .anyMatch(e -> e.getAggregateId().equals(tx.getId().toString()) && e.getEventType().equals("PAYMENT_SETTLED"));

        Assertions.assertTrue(hasMatchingEvent, "Must find OutboxEvent corresponding to the executed transfer transaction");
    }
}

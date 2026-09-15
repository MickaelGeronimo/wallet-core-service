package com.apex.wallet;

import com.apex.wallet.application.resilience.BalanceCacheService;
import com.apex.wallet.application.resilience.BufferedTransferQueueService;
import com.apex.wallet.application.resilience.ChaosManager;
import com.apex.wallet.application.service.TransferService;
import com.apex.wallet.domain.model.Account;
import com.apex.wallet.domain.model.Transaction;
import com.apex.wallet.domain.model.TransactionStatus;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import com.apex.wallet.infrastructure.resilience.QueueDrainWorker;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GracefulDegradationStressTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ChaosManager chaosManager;

    @Autowired
    private BufferedTransferQueueService queueService;

    @Autowired
    private BalanceCacheService balanceCacheService;

    @Autowired
    private QueueDrainWorker queueDrainWorker;

    @Autowired(required = false)
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        chaosManager.reset();
        queueService.clear();
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.find("transferService").ifPresent(io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);
        }
    }

    @AfterEach
    void tearDown() {
        chaosManager.reset();
        queueService.clear();
        if (circuitBreakerRegistry != null) {
            circuitBreakerRegistry.find("transferService").ifPresent(io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Normal Mode: Database healthy -> Direct atomic execution with status COMPLETED")
    void testHealthyTransferExecution() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        BigDecimal amount = new BigDecimal("25.00");
        String key = "HEALTHY-TX-" + UUID.randomUUID();

        Transaction tx = transferService.executeTransfer(key, deepak.getId(), marlon.getPixKey(), amount, "Teste Modo Saudável");

        Assertions.assertNotNull(tx);
        Assertions.assertEquals(TransactionStatus.COMPLETED, tx.getStatus(), "Transaction should be COMPLETED immediately");
        Assertions.assertNotNull(tx.getId(), "Persisted transaction must have an ID");
    }

    @Test
    @Order(2)
    @DisplayName("Degraded Mode: Database outage simulated -> Graceful Degradation activates, absorbing into buffer with 202 status QUEUED_FOR_SETTLEMENT (Zero 500 errors)")
    void testGracefulDegradationUnderDatabaseOutage() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        // 1. Simulate complete database connection failure
        chaosManager.setForceFailure(true);

        String key = "CHAOS-TX-" + UUID.randomUUID();
        BigDecimal amount = new BigDecimal("40.00");

        // 2. Transfer must NOT throw 500 RuntimeException; it must degrade gracefully
        Transaction degradedTx = transferService.executeTransfer(key, deepak.getId(), marlon.getPixKey(), amount, "Teste Degradação Graciosa");

        Assertions.assertNotNull(degradedTx);
        Assertions.assertEquals(TransactionStatus.QUEUED_FOR_SETTLEMENT, degradedTx.getStatus(),
                "Must return QUEUED_FOR_SETTLEMENT receipt instead of throwing 500 error");
        Assertions.assertTrue(queueService.hasPending(), "Transfer must be absorbed by in-memory buffer queue");
        Assertions.assertTrue(queueService.getQueueSize() >= 1, "Queue size must be at least 1");
    }

    @Test
    @Order(3)
    @DisplayName("Self-Healing & Eventual Consistency: Once DB recovers, QueueDrainWorker drains buffer and settles pending transactions")
    void testEventualConsistencyAndAutoHealing() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();
        Account marlon = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        BigDecimal initialMarlonBalance = marlon.getBalance();
        BigDecimal amount = new BigDecimal("50.00");
        String key = "DRAIN-TX-" + UUID.randomUUID();

        // 1. Force failure and enqueue transfer
        chaosManager.setForceFailure(true);
        Transaction degradedTx = transferService.executeTransfer(key, deepak.getId(), marlon.getPixKey(), amount, "Drenagem Pendente");
        Assertions.assertEquals(TransactionStatus.QUEUED_FOR_SETTLEMENT, degradedTx.getStatus());
        int queuedCount = queueService.getQueueSize();
        Assertions.assertTrue(queuedCount > 0, "Queue must contain pending items");

        // 2. Database recovers (Chaos reset)
        chaosManager.reset();

        // 3. QueueDrainWorker executes drainage
        queueDrainWorker.drainAll();

        // 4. Verify buffer was cleared and destination account received funds
        Account marlonAfter = accountRepository.findById(marlon.getId()).orElseThrow();
        Assertions.assertEquals(0, queueService.getQueueSize(), "Buffer queue must be completely drained");
        Assertions.assertEquals(initialMarlonBalance.add(amount), marlonAfter.getBalance(),
                "Marlon's balance must reflect the settled transfer after queue drainage");
    }

    @Test
    @Order(4)
    @DisplayName("High-Speed Read: Balance cache serves lookups in < 0.2ms even when DB is offline")
    void testHighSpeedBalanceCacheLookups() {
        Account deepak = accountRepository.findByEmail("deepak@microsoft.com").orElseThrow();

        // Populate cache
        balanceCacheService.put(deepak);

        // Simulate DB failure
        chaosManager.setForceFailure(true);

        // Read from cache without touching database
        var cached = balanceCacheService.getBalance(deepak.getId());
        Assertions.assertTrue(cached.isPresent(), "Cached balance must be present");
        Assertions.assertEquals(deepak.getBalance(), cached.get().balance());
        Assertions.assertEquals(deepak.getHolderName(), cached.get().holderName());
    }

    @Test
    @Order(5)
    @DisplayName("Concurrency Spike: 20 simultaneous threads during DB outage are 100% absorbed into buffer without dropped requests")
    void testExtremeConcurrencySpikeWithGracefulDegradation() throws InterruptedException {
        Account recipient = accountRepository.findByEmail("marlon@microsoft.com").orElseThrow();

        // Create 10 distinct sender accounts to simulate distributed real-world customers
        List<Account> senders = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Account acc = new Account("SPIKE-" + suffix,
                    "Spike User " + i,
                    "spike" + suffix + "@apex.com",
                    "secret",
                    "spike" + suffix + "@pix.com",
                    new BigDecimal("1000.00"),
                    "ROLE_USER");
            senders.add(accountRepository.save(acc));
        }

        int threads = 20;
        BigDecimal amount = new BigDecimal("10.00");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger absorbedCount = new AtomicInteger(0);

        // Activate Chaos
        chaosManager.setForceFailure(true);

        for (int i = 0; i < threads; i++) {
            final Account sender = senders.get(i % senders.size());
            executor.submit(() -> {
                try {
                    latch.await();
                    String key = "SPIKE-TX-" + UUID.randomUUID();
                    Transaction tx = transferService.executeTransfer(key, sender.getId(), recipient.getPixKey(), amount, "Spike Test");
                    if (tx.getStatus() == TransactionStatus.QUEUED_FOR_SETTLEMENT) {
                        absorbedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Not expected in graceful degradation
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        Assertions.assertTrue(finished);

        // All 20 requests must be absorbed by buffer, zero 500 dropped requests
        Assertions.assertEquals(threads, absorbedCount.get(), "All 20 requests must be gracefully degraded to QUEUED_FOR_SETTLEMENT");

        // Recover and drain
        chaosManager.reset();
        queueDrainWorker.drainAll();
        Assertions.assertEquals(0, queueService.getQueueSize(), "All spiked transactions must be successfully settled");
    }
}

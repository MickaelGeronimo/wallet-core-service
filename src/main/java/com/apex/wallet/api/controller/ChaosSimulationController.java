package com.apex.wallet.api.controller;

import com.apex.wallet.application.resilience.BalanceCacheService;
import com.apex.wallet.application.resilience.BufferedTransferQueueService;
import com.apex.wallet.application.resilience.ChaosManager;
import com.apex.wallet.infrastructure.resilience.QueueDrainWorker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for Chaos Engineering and Graceful Degradation Testing.
 * Allows simulating:
 * 1. Artificial DB latency (connection pool exhaustion / Black Friday spikes)
 * 2. Complete DB connection failure (HikariCP / Network partition)
 * 3. Operational status inspection (Circuit Breaker state, Buffer Queue size, Balance Cache)
 * 4. Manual queue drainage trigger
 */
@RestController
@RequestMapping("/api/chaos")
public class ChaosSimulationController {

    private final ChaosManager chaosManager;
    private final BufferedTransferQueueService queueService;
    private final BalanceCacheService balanceCacheService;
    private final QueueDrainWorker queueDrainWorker;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ChaosSimulationController(ChaosManager chaosManager,
                                     BufferedTransferQueueService queueService,
                                     BalanceCacheService balanceCacheService,
                                     QueueDrainWorker queueDrainWorker,
                                     @Autowired(required = false) CircuitBreakerRegistry circuitBreakerRegistry) {
        this.chaosManager = chaosManager;
        this.queueService = queueService;
        this.balanceCacheService = balanceCacheService;
        this.queueDrainWorker = queueDrainWorker;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @PostMapping("/db-latency")
    public ResponseEntity<Map<String, Object>> setDatabaseLatency(@RequestParam(defaultValue = "1000") long delayMs) {
        chaosManager.setDatabaseDelay(delayMs);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "SET_DATABASE_LATENCY");
        response.put("artificialDelayMs", delayMs);
        response.put("status", delayMs > 0 ? "CHAOS_ACTIVE" : "NORMAL");
        response.put("message", "Atraso artificial de " + delayMs + "ms configurado no banco de dados.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/force-failure")
    public ResponseEntity<Map<String, Object>> setForceFailure(@RequestParam(defaultValue = "true") boolean fail) {
        chaosManager.setForceFailure(fail);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "FORCE_DATABASE_FAILURE");
        response.put("forceFailure", fail);
        response.put("status", fail ? "SIMULATING_DATABASE_OUTAGE" : "NORMAL");
        response.put("message", fail
                ? "Falha total no banco ativada! Transferências serão retidas no buffer via Degradação Graciosa (202 Accepted)."
                : "Falha desativada. Banco normalizado.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        chaosManager.reset();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "RESET_ALL_CHAOS");
        response.put("status", "NORMAL");
        response.put("message", "Todas as condições de caos foram resetadas para operação normal.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getChaosStatus() {
        boolean forceFail = chaosManager.isForceFailure();
        long delay = chaosManager.getArtificialDatabaseDelayMs();
        int queueSize = queueService.getQueueSize();
        int cachedCount = balanceCacheService.getCachedAccountCount();

        String circuitState = "UNKNOWN";
        if (circuitBreakerRegistry != null) {
            circuitState = circuitBreakerRegistry.find("transferService")
                    .map(cb -> cb.getState().name())
                    .orElse("NOT_CONFIGURED");
        }

        String operationalMode = (forceFail || delay > 500) ? "DEGRADED" : "HEALTHY";

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("operationalMode", operationalMode);
        status.put("forceDatabaseFailure", forceFail);
        status.put("artificialDatabaseDelayMs", delay);
        status.put("bufferQueuePendingCount", queueSize);
        status.put("deadLetterQueueCount", queueService.getDeadLetterQueueSize());
        status.put("balanceCacheEntriesCount", cachedCount);
        status.put("circuitBreakerState", circuitState);
        status.put("systemResilience", "Graceful Degradation (Queue Buffering + In-Memory Fast Cache)");

        return ResponseEntity.ok(status);
    }

    @PostMapping("/drain-queue")
    public ResponseEntity<Map<String, Object>> manualDrainQueue() {
        int countBefore = queueService.getQueueSize();
        queueDrainWorker.drainAll();
        int countAfter = queueService.getQueueSize();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "MANUAL_QUEUE_DRAIN");
        response.put("processedItems", countBefore - countAfter);
        response.put("remainingInQueue", countAfter);
        response.put("message", "Dreno de fila assíncrona executado com sucesso.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dead-letter-items")
    public ResponseEntity<java.util.List<com.apex.wallet.application.resilience.QueuedTransferItem>> getDeadLetterItems() {
        return ResponseEntity.ok(queueService.getDeadLetterItems());
    }

    @PostMapping("/replay-dead-letter")
    public ResponseEntity<Map<String, Object>> replayDeadLetter() {
        int replayed = queueService.replayDeadLetterQueue();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("action", "REPLAY_DEAD_LETTER_QUEUE");
        response.put("replayedCount", replayed);
        response.put("bufferQueuePendingCount", queueService.getQueueSize());
        response.put("deadLetterQueueCount", queueService.getDeadLetterQueueSize());
        response.put("message", "Itens da Dead Letter Queue reenviados com sucesso para a fila de contingência.");
        return ResponseEntity.ok(response);
    }
}

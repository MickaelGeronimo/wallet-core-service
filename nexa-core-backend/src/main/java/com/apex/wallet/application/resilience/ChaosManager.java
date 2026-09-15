package com.apex.wallet.application.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chaos Engineering Manager for testing Resilience, Latency and Circuit Breakers.
 */
@Component
public class ChaosManager {

    private static final Logger log = LoggerFactory.getLogger(ChaosManager.class);

    private final AtomicLong artificialDatabaseDelayMs = new AtomicLong(0);
    private final AtomicBoolean forceDatabaseFailure = new AtomicBoolean(false);

    public void setDatabaseDelay(long delayMs) {
        this.artificialDatabaseDelayMs.set(delayMs);
        log.warn("CHAOS ACTIVATED: Atraso artificial no banco de dados configurado para {}ms", delayMs);
    }

    public void setForceFailure(boolean fail) {
        this.forceDatabaseFailure.set(fail);
        log.warn("CHAOS ACTIVATED: Falha forçada no banco de dados configurada para {}", fail);
    }

    public void reset() {
        this.artificialDatabaseDelayMs.set(0);
        this.forceDatabaseFailure.set(false);
        log.info("CHAOS RESET: Todas as condições de caos foram normalizadas.");
    }

    public void applyChaosIfActive() {
        if (forceDatabaseFailure.get()) {
            throw new RuntimeException("Simulação de Falha de Conexão com o Banco de Dados (HikariCP Connection Timeout)");
        }
        long delay = artificialDatabaseDelayMs.get();
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public long getArtificialDatabaseDelayMs() {
        return artificialDatabaseDelayMs.get();
    }

    public boolean isForceFailure() {
        return forceDatabaseFailure.get();
    }
}

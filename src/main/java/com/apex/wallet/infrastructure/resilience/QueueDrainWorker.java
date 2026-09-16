package com.apex.wallet.infrastructure.resilience;

import com.apex.wallet.application.resilience.BufferedTransferQueueService;
import com.apex.wallet.application.resilience.ChaosManager;
import com.apex.wallet.application.resilience.QueuedTransferItem;
import com.apex.wallet.application.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Background Worker that drains the buffering queue once primary database pressure subsides.
 * Demonstrates Self-Healing and Eventual Consistency in Real-Time Banking.
 */
@Component
public class QueueDrainWorker {

    private static final Logger log = LoggerFactory.getLogger(QueueDrainWorker.class);

    private final BufferedTransferQueueService queueService;
    private final ChaosManager chaosManager;
    private final TransferService transferService;

    public QueueDrainWorker(BufferedTransferQueueService queueService,
                            ChaosManager chaosManager,
                            @Lazy TransferService transferService) {
        this.queueService = queueService;
        this.chaosManager = chaosManager;
        this.transferService = transferService;
    }

    @Scheduled(fixedDelay = 2000)
    public void drainQueue() {
        if (!queueService.hasPending()) {
            return;
        }

        // If the database is currently failing or severely delayed, wait before draining
        if (chaosManager.isForceFailure() || chaosManager.getArtificialDatabaseDelayMs() > 1000) {
            log.info("QueueDrainWorker: Banco de dados ainda sob estresse. Aguardando para drenar {} itens...", queueService.getQueueSize());
            return;
        }

        List<QueuedTransferItem> batch = queueService.pollBatch(100);
        log.info("QueueDrainWorker: Drenando lote de {} transferências enfileiradas...", batch.size());

        for (QueuedTransferItem item : batch) {
            try {
                transferService.executeSettlementDirect(
                        item.idempotencyKey(),
                        item.sourceAccountId(),
                        item.targetPixKey(),
                        item.amount(),
                        item.description() + " [Processado via Fila de Drenagem]"
                );
                log.info("QueueDrainWorker: Transação {} liquidada com sucesso!", item.idempotencyKey());
            } catch (Exception e) {
                log.error("QueueDrainWorker: Falha ao processar item {}: {}", item.idempotencyKey(), e.getMessage());
                if (item.retryCount() < 3) {
                    queueService.enqueue(item.withIncrementedRetry());
                } else {
                    queueService.routeToDeadLetter(item);
                }
            }
        }
    }

    public void drainAll() {
        while (queueService.hasPending() && !chaosManager.isForceFailure()) {
            drainQueue();
        }
    }
}

package com.apex.wallet.infrastructure.outbox;

import com.apex.wallet.application.resilience.ChaosManager;
import com.apex.wallet.domain.outbox.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxRelayWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayWorker.class);

    private final OutboxEventRepository outboxRepo;
    private final ChaosManager chaosManager;

    public OutboxRelayWorker(OutboxEventRepository outboxRepo, ChaosManager chaosManager) {
        this.outboxRepo = outboxRepo;
        this.chaosManager = chaosManager;
    }

    @Scheduled(fixedDelay = 2500)
    @Transactional
    public void processOutboxEvents() {
        if (chaosManager.isForceFailure()) {
            return;
        }

        List<OutboxEvent> pending = outboxRepo.findPendingForDispatch(PageRequest.of(0, 20));
        if (pending.isEmpty()) {
            return;
        }

        log.info("OutboxRelayWorker: Disparando {} eventos pendentes para o barramento assíncrono (Kafka/Audit)...", pending.size());

        for (OutboxEvent event : pending) {
            try {
                // Simulação de publicação atômica no broker Kafka / EventBus
                log.info("Outbox publicado [{}]: aggregateId={}, type={}",
                        event.getEventType(), event.getAggregateId(), event.getAggregateType());

                event.markPublished();
                outboxRepo.save(event);
            } catch (Exception e) {
                log.error("Falha ao publicar outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed(e.getMessage());
                outboxRepo.save(event);
            }
        }
    }
}

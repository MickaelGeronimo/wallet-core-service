package com.apex.wallet.application.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-Memory Asynchronous Buffering Queue for Graceful Degradation during traffic spikes.
 * When the database connection pool is congested or the Circuit Breaker trips,
 * incoming transfers are absorbed here instead of failing with HTTP 500/503.
 */
@Service
public class BufferedTransferQueueService {

    private static final Logger log = LoggerFactory.getLogger(BufferedTransferQueueService.class);

    public static final int MAX_BUFFER_CAPACITY = 25_000;

    private final Queue<QueuedTransferItem> bufferQueue = new ConcurrentLinkedQueue<>();

    public void enqueue(QueuedTransferItem item) {
        if (bufferQueue.size() >= MAX_BUFFER_CAPACITY) {
            throw new IllegalStateException("Capacidade máxima do buffer de contingência atingida. Tente novamente em instantes.");
        }
        bufferQueue.offer(item);
        log.warn("DEGRAÇÃO GRACIOSA ATIVADA: Transferência {} enfileirada no buffer assíncrono. Tamanho atual da fila: {}",
                item.idempotencyKey(), bufferQueue.size());
    }

    public List<QueuedTransferItem> pollBatch(int maxBatchSize) {
        List<QueuedTransferItem> batch = new ArrayList<>();
        while (!bufferQueue.isEmpty() && batch.size() < maxBatchSize) {
            QueuedTransferItem item = bufferQueue.poll();
            if (item != null) {
                batch.add(item);
            }
        }
        return batch;
    }

    public int getQueueSize() {
        return bufferQueue.size();
    }

    public boolean hasPending() {
        return !bufferQueue.isEmpty();
    }

    private final Queue<QueuedTransferItem> deadLetterQueue = new ConcurrentLinkedQueue<>();

    public void routeToDeadLetter(QueuedTransferItem item) {
        deadLetterQueue.offer(item);
        log.error("DLQ ATIVADA: Transferência {} encaminhada para a Dead Letter Queue após esgotar tentativas.", item.idempotencyKey());
    }

    public int getDeadLetterQueueSize() {
        return deadLetterQueue.size();
    }

    public List<QueuedTransferItem> getDeadLetterItems() {
        return new ArrayList<>(deadLetterQueue);
    }

    public int replayDeadLetterQueue() {
        int count = 0;
        QueuedTransferItem item;
        while ((item = deadLetterQueue.poll()) != null) {
            // Re-enqueue with reset retry count to give it another processing chance
            bufferQueue.offer(new QueuedTransferItem(
                    item.idempotencyKey(),
                    item.sourceAccountId(),
                    item.targetPixKey(),
                    item.amount(),
                    item.description(),
                    Instant.now(),
                    0
            ));
            count++;
        }
        log.info("DLQ REPLAY: {} itens re-enfileirados da DLQ para a fila principal de contingência.", count);
        return count;
    }

    public void clear() {
        bufferQueue.clear();
        deadLetterQueue.clear();
    }
}

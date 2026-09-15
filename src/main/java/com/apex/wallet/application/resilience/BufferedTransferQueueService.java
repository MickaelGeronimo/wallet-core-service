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

    private final Queue<QueuedTransferItem> bufferQueue = new ConcurrentLinkedQueue<>();

    public void enqueue(QueuedTransferItem item) {
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

    public void clear() {
        bufferQueue.clear();
    }
}

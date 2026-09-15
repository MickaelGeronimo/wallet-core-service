package com.apex.wallet.application.resilience;

import java.math.BigDecimal;
import java.time.Instant;

public record QueuedTransferItem(
        String idempotencyKey,
        Long sourceAccountId,
        String targetPixKey,
        BigDecimal amount,
        String description,
        Instant enqueuedAt,
        int retryCount
) {
    public QueuedTransferItem withIncrementedRetry() {
        return new QueuedTransferItem(idempotencyKey, sourceAccountId, targetPixKey, amount, description, enqueuedAt, retryCount + 1);
    }
}

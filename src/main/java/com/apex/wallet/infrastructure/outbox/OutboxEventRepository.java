package com.apex.wallet.infrastructure.outbox;

import com.apex.wallet.domain.outbox.OutboxEvent;
import com.apex.wallet.domain.outbox.OutboxStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Polls pending or retryable failed events using pessimistic row-level locking with
     * SKIP LOCKED (timeout = -2). Prevents multiple concurrent pods/workers in horizontal
     * scale from claiming or publishing the exact same events simultaneously.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT e FROM OutboxEvent e WHERE e.status IN ('PENDING', 'FAILED') AND e.retryCount < 3 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingForDispatch(Pageable pageable);

    List<OutboxEvent> findByStatus(OutboxStatus status);
}

package com.apex.wallet.infrastructure.outbox;

import com.apex.wallet.domain.outbox.OutboxEvent;
import com.apex.wallet.domain.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status IN ('PENDING', 'FAILED') AND e.retryCount < 3 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingForDispatch(Pageable pageable);

    List<OutboxEvent> findByStatus(OutboxStatus status);
}

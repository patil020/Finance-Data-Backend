package com.financeapp.record.outbox;

import com.financeapp.record.outbox.OutboxEvent.OutboxStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop50ByStatusInOrderByCreatedAtAsc(Collection<OutboxStatus> statuses);
}

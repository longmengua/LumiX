/*
 * 檔案用途：檢查 durable outbox rows 是否缺少對應 domain-state transition。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.OutboxDomainStateConsistencyIssue;
import com.example.exchange.domain.model.dto.OutboxDomainStateConsistencyReport;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.repository.OrderLifecycleProjectionStore;
import com.example.exchange.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxDomainStateConsistencyService {

    private final OutboxRepository outboxRepository;
    private final OrderLifecycleProjectionStore orderLifecycleProjectionStore;

    public OutboxDomainStateConsistencyReport inspectLatest(int limit) {
        List<OutboxEvent> events = outboxRepository.latest(Math.max(1, limit));
        List<OutboxDomainStateConsistencyIssue> issues = new ArrayList<>();
        for (OutboxEvent event : events) {
            inspect(event, issues);
        }
        return new OutboxDomainStateConsistencyReport(
                events.size(),
                issues.size(),
                Instant.now(),
                issues
        );
    }

    private void inspect(OutboxEvent event, List<OutboxDomainStateConsistencyIssue> issues) {
        if (event == null) {
            return;
        }
        if ("order.lifecycle".equals(event.getTopic())) {
            String orderId = orderIdFromEventKey(event.getEventKey());
            if (orderId == null || orderLifecycleProjectionStore.findByOrderId(orderId).isEmpty()) {
                issues.add(issue(event, "MISSING_ORDER_LIFECYCLE_PROJECTION",
                        "outbox order.lifecycle row has no matching order lifecycle projection"));
            }
        }
    }

    private static String orderIdFromEventKey(String eventKey) {
        if (eventKey == null || eventKey.isBlank()) {
            return null;
        }
        int separator = eventKey.indexOf(':');
        if (separator < 0 || separator == eventKey.length() - 1) {
            return null;
        }
        return eventKey.substring(separator + 1);
    }

    private static OutboxDomainStateConsistencyIssue issue(
            OutboxEvent event,
            String issueType,
            String detail
    ) {
        return new OutboxDomainStateConsistencyIssue(
                event.getId(),
                event.getTopic(),
                event.getEventKey(),
                event.getEventType(),
                event.getStatus() == null ? null : event.getStatus().name(),
                issueType,
                detail
        );
    }
}

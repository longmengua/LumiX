/*
 * 檔案用途：統一定義可送往營運 alert backend 的告警 payload 與內建 routing contract。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class OperationalAlert {

    private final String alertName;

    private final AlertSeverity severity;

    private final String route;

    private final String summary;

    private final String entityId;

    private final String runbook;

    private final Map<String, String> labels;

    private final Map<String, String> details;

    private final String requestId;

    private final String correlationId;

    private final Instant triggeredAt;
    public OperationalAlert(String alertName, AlertSeverity severity, String route, String summary, String entityId, String runbook, Map<String, String> labels, Map<String, String> details, String requestId, String correlationId, Instant triggeredAt) {
        alertName = requireText(alertName, "alertName");
        severity = severity == null ? AlertSeverity.WARNING : severity;
        route = requireText(route, "route");
        summary = requireText(summary, "summary");
        entityId = normalize(entityId);
        runbook = normalize(runbook);
        labels = labels == null ? Map.of() : Map.copyOf(labels);
        details = details == null ? Map.of() : Map.copyOf(details);
        requestId = normalize(requestId);
        correlationId = normalize(correlationId);
        triggeredAt = triggeredAt == null ? Instant.now() : triggeredAt;
    
        this.alertName = alertName;
        this.severity = severity;
        this.route = route;
        this.summary = summary;
        this.entityId = entityId;
        this.runbook = runbook;
        this.labels = labels;
        this.details = details;
        this.requestId = requestId;
        this.correlationId = correlationId;
        this.triggeredAt = triggeredAt;
    }

    public OperationalAlert withTrace(String requestId, String correlationId) {
        return new OperationalAlert(
                alertName,
                severity,
                route,
                summary,
                entityId,
                runbook,
                labels,
                details,
                firstNonBlank(this.requestId, requestId),
                firstNonBlank(this.correlationId, correlationId),
                triggeredAt
        );
    }

    public static OperationalAlert matchingHalt(String symbol, String reason) {
        return new OperationalAlert(
                "matching_halt",
                AlertSeverity.CRITICAL,
                "ops.matching",
                "Matching is halted or sequencer ownership is unsafe",
                symbol,
                "docs/reliability/matching-sequencer-runbook.md",
                Map.of("symbol", safe(symbol)),
                Map.of("reason", safe(reason)),
                null,
                null,
                Instant.now()
        );
    }

    public static OperationalAlert kafkaLag(String topic, int partition, long lag, AlertSeverity severity) {
        return new OperationalAlert(
                "kafka_lag",
                severity,
                "ops.kafka",
                "Kafka consumer lag exceeded the alert threshold",
                topic + ":" + partition,
                "docs/architecture/kafka-topics.md",
                Map.of("topic", safe(topic), "partition", String.valueOf(partition)),
                Map.of("lag", String.valueOf(lag)),
                null,
                null,
                Instant.now()
        );
    }

    public static OperationalAlert dlqBuildup(String eventType, long deadCount, long oldestPendingSeconds) {
        return new OperationalAlert(
                "dlq_buildup",
                AlertSeverity.WARNING,
                "ops.outbox",
                "Outbox DLQ or pending backlog is growing",
                eventType,
                "docs/operations/outbox-runbook.md",
                Map.of("eventType", safe(eventType)),
                Map.of("deadCount", String.valueOf(deadCount), "oldestPendingSeconds", String.valueOf(oldestPendingSeconds)),
                null,
                null,
                Instant.now()
        );
    }

    public static OperationalAlert reconciliationFailure(String reportId, int errors, int warnings) {
        return new OperationalAlert(
                "reconciliation_failure",
                AlertSeverity.CRITICAL,
                "ops.reconciliation",
                "Reconciliation report contains unresolved errors",
                reportId,
                "docs/reliability/finance-operator-runbook.md",
                Map.of("reportId", safe(reportId)),
                Map.of("errors", String.valueOf(errors), "warnings", String.valueOf(warnings)),
                null,
                null,
                Instant.now()
        );
    }

    public static OperationalAlert externalApiErrorRate(String provider, String operation, long failures, long attempts) {
        return new OperationalAlert(
                "external_api_error_rate",
                AlertSeverity.WARNING,
                "ops.external-api",
                "External API failures or unresolved outcomes exceeded the alert threshold",
                provider + ":" + operation,
                "docs/operations/observability.md",
                Map.of("provider", safe(provider), "operation", safe(operation)),
                Map.of("failures", String.valueOf(failures), "attempts", String.valueOf(attempts)),
                null,
                null,
                Instant.now()
        );
    }

    public static OperationalAlert unbalancedAssets(String reportId, String asset, String imbalance) {
        return new OperationalAlert(
                "unbalanced_assets",
                AlertSeverity.CRITICAL,
                "ops.finance",
                "Ledger or daily finance report is not balanced",
                reportId,
                "docs/reliability/finance-operator-runbook.md",
                Map.of("reportId", safe(reportId), "asset", safe(asset)),
                Map.of("imbalance", safe(imbalance)),
                null,
                null,
                Instant.now()
        );
    }

    public enum AlertSeverity {
        WARNING,
        CRITICAL
    }

    private static String requireText(String value, String field) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonBlank(String first, String second) {
        String normalized = normalize(first);
        return normalized != null ? normalized : normalize(second);
    }

    private static String safe(String value) {
        String normalized = normalize(value);
        return normalized == null ? "unknown" : normalized;
    }

    public String alertName() {
        return alertName;
    }

    public AlertSeverity severity() {
        return severity;
    }

    public String route() {
        return route;
    }

    public String summary() {
        return summary;
    }

    public String entityId() {
        return entityId;
    }

    public String runbook() {
        return runbook;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> details() {
        return details;
    }

    public String requestId() {
        return requestId;
    }

    public String correlationId() {
        return correlationId;
    }

    public Instant triggeredAt() {
        return triggeredAt;
    }
}
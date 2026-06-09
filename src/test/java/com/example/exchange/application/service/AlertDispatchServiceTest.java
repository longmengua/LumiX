/*
 * 檔案用途：測試營運 alert backend dispatch 的停用、送達與失敗隔離 contract。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AlertDispatchResult;
import com.example.exchange.domain.model.dto.OperationalAlert;
import com.example.exchange.infra.config.AlertBackendProperties;
import com.example.exchange.infra.tracing.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertDispatchServiceTest {

    @AfterEach
    void clearTrace() {
        TraceContext.clear();
    }

    @Test
    @DisplayName("disabled alert backend 只回傳 skipped，不呼叫外部 transport")
    void disabledBackendSkipsExternalDispatch() {
        AlertBackendProperties properties = new AlertBackendProperties();
        RecordingTransport transport = new RecordingTransport(AlertDispatchResult.delivered(202));
        AlertDispatchService service = new AlertDispatchService(properties, transport);

        // 場景：production 尚未配置 PagerDuty/Slack webhook 時，告警不應造成任何外部副作用或交易流程失敗。
        AlertDispatchResult result = service.dispatch(OperationalAlert.matchingHalt("BTCUSDT", "NO_ACTIVE_OWNER"));

        assertThat(result.status()).isEqualTo(AlertDispatchResult.AlertDispatchStatus.SKIPPED);
        assertThat(result.message()).isEqualTo("ALERT_BACKEND_DISABLED");
        assertThat(transport.calls).isEmpty();
    }

    @Test
    @DisplayName("enabled alert backend 送出帶 route、severity 與 tracing id 的 payload")
    void enabledBackendPostsTracedAlertPayload() {
        AlertBackendProperties properties = enabledProperties();
        RecordingTransport transport = new RecordingTransport(AlertDispatchResult.delivered(202));
        AlertDispatchService service = new AlertDispatchService(properties, transport);
        TraceContext.put("request-1", "corr-1");

        // 場景：alert backend 需要 route/entity/tracing 欄位，才能 group repeated alerts 並連回 request logs。
        AlertDispatchResult result = service.dispatch(OperationalAlert.kafkaLag(
                "order-events",
                3,
                12_500,
                OperationalAlert.AlertSeverity.CRITICAL
        ));

        assertThat(result.status()).isEqualTo(AlertDispatchResult.AlertDispatchStatus.DELIVERED);
        assertThat(transport.calls).hasSize(1);
        RecordedCall call = transport.calls.get(0);
        assertThat(call.webhookUrl).isEqualTo("https://alerts.example.test/hook");
        assertThat(call.timeoutMs).isEqualTo(1500);
        assertThat(call.alert.alertName()).isEqualTo("kafka_lag");
        assertThat(call.alert.severity()).isEqualTo(OperationalAlert.AlertSeverity.CRITICAL);
        assertThat(call.alert.route()).isEqualTo("ops.kafka");
        assertThat(call.alert.entityId()).isEqualTo("order-events:3");
        assertThat(call.alert.requestId()).isEqualTo("request-1");
        assertThat(call.alert.correlationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("transport failure 會回傳 failed，不向呼叫端丟出例外")
    void transportFailureIsReportedWithoutThrowing() {
        AlertBackendProperties properties = enabledProperties();
        AlertDispatchService service = new AlertDispatchService(
                properties,
                new RecordingTransport(AlertDispatchResult.failed(500, "ALERT_BACKEND_HTTP_500"))
        );

        // 場景：alert backend 當機時，交易/對帳流程只能留下 dispatch 結果，不能因通知失敗而 rollback。
        AlertDispatchResult result = service.dispatch(OperationalAlert.unbalancedAssets(
                "daily-2026-06-10",
                "USDT",
                "12.34"
        ));

        assertThat(result.status()).isEqualTo(AlertDispatchResult.AlertDispatchStatus.FAILED);
        assertThat(result.statusCode()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("ALERT_BACKEND_HTTP_500");
    }

    @Test
    @DisplayName("內建 alert factory 固定六類 production route")
    void builtInAlertFactoriesUseDocumentedRoutes() {
        assertThat(OperationalAlert.matchingHalt("ETHUSDT", "DUPLICATE_OWNER").route()).isEqualTo("ops.matching");
        assertThat(OperationalAlert.kafkaLag("topic", 0, 1001, OperationalAlert.AlertSeverity.WARNING).route()).isEqualTo("ops.kafka");
        assertThat(OperationalAlert.dlqBuildup("ORDER_ACCEPTED", 5, 600).route()).isEqualTo("ops.outbox");
        assertThat(OperationalAlert.reconciliationFailure("report-1", 1, 0).route()).isEqualTo("ops.reconciliation");
        assertThat(OperationalAlert.externalApiErrorRate("clob", "cancel", 3, 10).route()).isEqualTo("ops.external-api");
        assertThat(OperationalAlert.unbalancedAssets("report-2", "USDT", "0.01").route()).isEqualTo("ops.finance");
    }

    private static AlertBackendProperties enabledProperties() {
        AlertBackendProperties properties = new AlertBackendProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://alerts.example.test/hook");
        properties.setTimeoutMs(1500);
        return properties;
    }

    private static final class RecordingTransport implements AlertDispatchService.AlertTransport {
        private final AlertDispatchResult result;
        private final List<RecordedCall> calls = new ArrayList<>();

        private RecordingTransport(AlertDispatchResult result) {
            this.result = result;
        }

        @Override
        public AlertDispatchResult post(String webhookUrl, OperationalAlert alert, int timeoutMs) {
            calls.add(new RecordedCall(webhookUrl, alert, timeoutMs));
            return result;
        }
    }

    private record RecordedCall(String webhookUrl, OperationalAlert alert, int timeoutMs) {
    }
}

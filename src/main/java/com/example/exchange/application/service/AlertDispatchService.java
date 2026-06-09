/*
 * 檔案用途：應用服務，將營運告警送往可配置 backend；失敗只記錄結果，不改變交易狀態。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.AlertDispatchResult;
import com.example.exchange.domain.model.dto.OperationalAlert;
import com.example.exchange.infra.config.AlertBackendProperties;
import com.example.exchange.infra.tracing.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDispatchService {

    private final AlertBackendProperties properties;
    private final AlertTransport alertTransport;

    public AlertDispatchResult dispatch(OperationalAlert alert) {
        OperationalAlert tracedAlert = withCurrentTrace(alert);
        if (!properties.isEnabled() || properties.getWebhookUrl() == null || properties.getWebhookUrl().isBlank()) {
            log.warn(
                    "OPERATIONAL_ALERT status=SKIPPED reason=ALERT_BACKEND_DISABLED alert={} severity={} route={} entityId={} requestId={} correlationId={}",
                    tracedAlert.alertName(),
                    tracedAlert.severity(),
                    tracedAlert.route(),
                    tracedAlert.entityId(),
                    tracedAlert.requestId(),
                    tracedAlert.correlationId()
            );
            return AlertDispatchResult.skipped("ALERT_BACKEND_DISABLED");
        }

        AlertDispatchResult result = alertTransport.post(
                properties.getWebhookUrl().trim(),
                tracedAlert,
                properties.getTimeoutMs()
        );
        if (result.status() == AlertDispatchResult.AlertDispatchStatus.DELIVERED) {
            log.info(
                    "OPERATIONAL_ALERT status=DELIVERED alert={} severity={} route={} entityId={} statusCode={}",
                    tracedAlert.alertName(),
                    tracedAlert.severity(),
                    tracedAlert.route(),
                    tracedAlert.entityId(),
                    result.statusCode()
            );
        } else {
            log.error(
                    "OPERATIONAL_ALERT status=FAILED alert={} severity={} route={} entityId={} statusCode={} message={}",
                    tracedAlert.alertName(),
                    tracedAlert.severity(),
                    tracedAlert.route(),
                    tracedAlert.entityId(),
                    result.statusCode(),
                    result.message()
            );
        }
        return result;
    }

    private static OperationalAlert withCurrentTrace(OperationalAlert alert) {
        Map<String, String> headers = TraceContext.currentHeaders();
        return alert.withTrace(
                headers.get(TraceContext.REQUEST_ID_HEADER),
                headers.get(TraceContext.CORRELATION_ID_HEADER)
        );
    }

    public interface AlertTransport {
        AlertDispatchResult post(String webhookUrl, OperationalAlert alert, int timeoutMs);
    }
}

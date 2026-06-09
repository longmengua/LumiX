/*
 * 檔案用途：測試 OperationalMetricsService 匯出到 Micrometer registry 的 meter baseline。
 */
package com.example.exchange.infra.metrics;

import com.example.exchange.application.service.OperationalMetricsService;
import com.example.exchange.domain.model.entity.Order;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalMetricsMeterBinderTest {

    @Test
    @DisplayName("Operational metrics 會註冊為 Prometheus 可抓取的 Micrometer meters")
    /**
     * 流程：先產生 filled order、DB latency、Redis latency、Kafka lag，再 bind 到 registry。
     * 期望：counter/gauge meter 能從既有 snapshot 讀到同一份 production metrics baseline。
     */
    void bindsOperationalMetricsToMeterRegistry() {
        OperationalMetricsService service = new OperationalMetricsService();
        service.recordOrderResult(Order.builder().status(Order.Status.FILLED).build(), service.startTimer());
        service.recordDatabaseLatencyMillis(12);
        service.recordRedisLatencyMillis(3);
        service.recordKafkaLag(7);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new OperationalMetricsMeterBinder(service).bindTo(registry);

        assertThat(registry.find("exchange.orders.requests").functionCounter().count()).isEqualTo(1.0);
        assertThat(registry.find("exchange.orders.status").tag("status", "filled").functionCounter().count())
                .isEqualTo(1.0);
        assertThat(registry.find("exchange.matching.fill.rate").gauge().value()).isEqualTo(1.0);
        assertThat(registry.find("exchange.database.latency.max").gauge().value()).isEqualTo(12.0);
        assertThat(registry.find("exchange.redis.latency.max").gauge().value()).isEqualTo(3.0);
        assertThat(registry.find("exchange.kafka.lag.max").gauge().value()).isEqualTo(7.0);
    }
}

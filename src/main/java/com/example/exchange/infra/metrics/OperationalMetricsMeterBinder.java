/*
 * 檔案用途：將既有營運 metrics snapshot 匯出為 Micrometer / Prometheus meters。
 */
package com.example.exchange.infra.metrics;

import com.example.exchange.application.service.OperationalMetricsService;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class OperationalMetricsMeterBinder implements MeterBinder {

    private final OperationalMetricsService operationalMetricsService;

    public OperationalMetricsMeterBinder(OperationalMetricsService operationalMetricsService) {
        this.operationalMetricsService = operationalMetricsService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registerCounters(registry);
        registerGauges(registry);
    }

    private void registerCounters(MeterRegistry registry) {
        counter(registry, "exchange.orders.requests", "Completed order requests recorded by the API flow",
                service -> service.snapshot().orderRequests());
        statusCounter(registry, "new", service -> service.snapshot().orderNew());
        statusCounter(registry, "partially_filled", service -> service.snapshot().orderPartiallyFilled());
        statusCounter(registry, "filled", service -> service.snapshot().orderFilled());
        statusCounter(registry, "rejected", service -> service.snapshot().orderRejected());
        statusCounter(registry, "canceled", service -> service.snapshot().orderCanceled());
        statusCounter(registry, "expired", service -> service.snapshot().orderExpired());
        counter(registry, "exchange.trade.events", "Observed trade events",
                service -> service.snapshot().tradeEvents());
        counter(registry, "exchange.matching.requests", "Matching attempts recorded from order flow",
                service -> service.snapshot().matchingRequests());
        counter(registry, "exchange.matching.rejected", "Rejected matching attempts",
                service -> service.snapshot().matchingRejected());
        counter(registry, "exchange.matching.filled", "Matching attempts that produced fills",
                service -> service.snapshot().matchingFilled());
    }

    private void registerGauges(MeterRegistry registry) {
        gauge(registry, "exchange.orders.latency.avg", "Average order latency in milliseconds",
                service -> service.snapshot().orderLatencyAvgMs());
        gauge(registry, "exchange.orders.latency.max", "Max order latency in milliseconds",
                service -> service.snapshot().orderLatencyMaxMs());
        gauge(registry, "exchange.matching.rejection.rate", "Matching rejection rate",
                service -> service.snapshot().matchingRejectionRate());
        gauge(registry, "exchange.matching.fill.rate", "Matching fill rate",
                service -> service.snapshot().matchingFillRate());
        gauge(registry, "exchange.matching.latency.avg", "Average matching latency in milliseconds",
                service -> service.snapshot().matchingLatencyAvgMs());
        gauge(registry, "exchange.matching.latency.max", "Max matching latency in milliseconds",
                service -> service.snapshot().matchingLatencyMaxMs());
        gauge(registry, "exchange.database.latency.avg", "Average database operation latency in milliseconds",
                service -> service.snapshot().databaseLatencyAvgMs());
        gauge(registry, "exchange.database.latency.max", "Max database operation latency in milliseconds",
                service -> service.snapshot().databaseLatencyMaxMs());
        gauge(registry, "exchange.redis.latency.avg", "Average Redis operation latency in milliseconds",
                service -> service.snapshot().redisLatencyAvgMs());
        gauge(registry, "exchange.redis.latency.max", "Max Redis operation latency in milliseconds",
                service -> service.snapshot().redisLatencyMaxMs());
        gauge(registry, "exchange.kafka.lag.total", "Total sampled Kafka consumer lag in messages",
                service -> service.snapshot().kafkaLagTotal());
        gauge(registry, "exchange.kafka.lag.max", "Max sampled Kafka consumer partition lag in messages",
                service -> service.snapshot().kafkaLagMax());
    }

    private void statusCounter(
            MeterRegistry registry,
            String status,
            MetricValue value
    ) {
        FunctionCounter.builder("exchange.orders.status", operationalMetricsService, value::read)
                .tag("status", status)
                .description("Completed orders by final status")
                .register(registry);
    }

    private void counter(
            MeterRegistry registry,
            String name,
            String description,
            MetricValue value
    ) {
        FunctionCounter.builder(name, operationalMetricsService, value::read)
                .description(description)
                .register(registry);
    }

    private void gauge(
            MeterRegistry registry,
            String name,
            String description,
            MetricValue value
    ) {
        Gauge.builder(name, operationalMetricsService, value::read)
                .description(description)
                .register(registry);
    }

    @FunctionalInterface
    private interface MetricValue {
        double read(OperationalMetricsService service);
    }
}

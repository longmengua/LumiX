/*
 * 檔案用途：Kafka 基礎設施 adapter，負責 domain event 的路由、發布與保存。
 */
package com.example.exchange.infra.kafka;

import com.example.exchange.domain.event.FundingSettled;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.event.PositionLiquidated;
import com.example.exchange.domain.event.TradeExecuted;

record KafkaEventRoute(String topic, String key) {

    static KafkaEventRoute from(Object event) {
        if (event instanceof TradeExecuted trade) {
            return new KafkaEventRoute("trade.executed", trade.symbol().code());
        }
        if (event instanceof OrderLifecycleEvent order) {
            String symbol = order.symbol() == null ? "UNKNOWN" : order.symbol().code();
            String orderId = order.orderId() == null ? "unknown-order" : order.orderId().toString();
            return new KafkaEventRoute("order.lifecycle", symbol + ":" + orderId);
        }
        if (event instanceof FundingSettled funding) {
            return new KafkaEventRoute("funding.settled", funding.symbol().code());
        }
        if (event instanceof PositionLiquidated liquidation) {
            return new KafkaEventRoute("position.liquidated", liquidation.symbol().code());
        }
        return new KafkaEventRoute("domain.events", event == null ? "null" : event.getClass().getSimpleName());
    }
}

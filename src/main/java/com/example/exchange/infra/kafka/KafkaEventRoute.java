package com.example.exchange.infra.kafka;

import com.example.exchange.domain.event.FundingSettled;
import com.example.exchange.domain.event.PositionLiquidated;
import com.example.exchange.domain.event.TradeExecuted;

record KafkaEventRoute(String topic, String key) {

    static KafkaEventRoute from(Object event) {
        if (event instanceof TradeExecuted trade) {
            return new KafkaEventRoute("trade.executed", trade.symbol().code());
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

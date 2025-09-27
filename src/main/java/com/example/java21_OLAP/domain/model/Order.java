package com.example.java21_OLAP.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order (訂單聚合)
 *
 * - id: UUID
 * - side: BUY / SELL
 * - type: LIMIT / MARKET
 * - price: 價格 (Market 可能為 null)
 * - qty: 剩餘數量
 */
@Data
public class Order {

    public enum Status { NEW, PARTIALLY_FILLED, FILLED, CANCELED }

    private final UUID id = UUID.randomUUID();
    private final long uid;
    private final Symbol symbol;
    private final OrderSide side;
    private final OrderType type;
    private final BigDecimal price;
    private BigDecimal qty;
    private Status status = Status.NEW;
    private final Instant ctime = Instant.now();

    public Order(long uid, Symbol symbol, OrderSide side, OrderType type, BigDecimal price, BigDecimal qty) {
        this.uid = uid;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.price = price;
        this.qty = qty;
    }

    /** 訂單被成交 (部分或全部) */
    public void fill(BigDecimal execQty) {
        qty = qty.subtract(execQty);
        status = qty.signum() == 0 ? Status.FILLED : Status.PARTIALLY_FILLED;
    }
}

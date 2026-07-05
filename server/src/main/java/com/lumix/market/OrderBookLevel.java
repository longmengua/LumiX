package com.lumix.market;

import com.lumix.common.MoneyAmount;

import java.util.Objects;

/**
 * 深度檔位模型。
 */
public record OrderBookLevel(
        MoneyAmount price,
        MoneyAmount quantity
) {

    public OrderBookLevel {
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("price must be greater than zero");
        }
        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
    }
}

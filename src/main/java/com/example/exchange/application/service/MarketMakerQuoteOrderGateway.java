/*
 * 檔案用途：應用層 gateway，將做市商 quote leg 轉成內部訂單。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import com.example.exchange.domain.model.enums.OrderSide;

import java.util.UUID;

public interface MarketMakerQuoteOrderGateway {

    int cancelOpenQuoteOrders(MarketMakerQuoteCommand command);

    UUID placePostOnlyLimit(MarketMakerQuoteCommand command, OrderSide side);
}

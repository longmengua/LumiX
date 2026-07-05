package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.RequestId;

import java.util.List;
import java.util.Optional;

/**
 * 現貨訂單服務契約。
 */
public interface SpotOrderService {

    // TODO: requires high-reasoning review before production use
    SpotOrderView placeOrder(SpotOrderRequest request);

    // TODO: requires high-reasoning review before production use
    boolean cancelOrder(RequestId requestId, UserId userId, String orderId);

    // TODO: requires high-reasoning review before production use
    Optional<SpotOrderView> getOrder(UserId userId, String orderId);

    // TODO: requires high-reasoning review before production use
    List<SpotOrderView> listOpenOrders(UserId userId, String symbol);

    // TODO: requires high-reasoning review before production use
    List<SpotOrderView> listOrderHistory(UserId userId, String symbol);

    // TODO: requires high-reasoning review before production use
    List<SpotTradeView> listTrades(UserId userId, String symbol);
}

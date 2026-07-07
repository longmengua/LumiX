package com.lumix.spot;

import com.lumix.account.UserId;
import com.lumix.common.RequestId;

import java.util.List;
import java.util.Optional;

/**
 * 現貨訂單服務契約。
 */
public interface SpotOrderService {

    // TODO(HUMAN_REVIEW_REQUIRED): 提交現貨訂單；正式版本必須先完成資金預留與 idempotency 檢查。
    SpotOrderView placeOrder(SpotOrderRequest request);

    // TODO(HUMAN_REVIEW_REQUIRED): 取消現貨訂單；正式版本需要與 matching / reservation 邊界一致。
    boolean cancelOrder(RequestId requestId, UserId userId, String orderId);

    // 查詢單筆訂單。
    Optional<SpotOrderView> getOrder(UserId userId, String orderId);

    // 查詢尚未完成的訂單。
    List<SpotOrderView> listOpenOrders(UserId userId, String symbol);

    // 查詢歷史訂單。
    List<SpotOrderView> listOrderHistory(UserId userId, String symbol);

    // 查詢成交紀錄。
    List<SpotTradeView> listTrades(UserId userId, String symbol);
}

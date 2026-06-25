/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.Order;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 撮合結果（相容擴充用 DTO）。
 *
 * 欄位說明：
 * - trades：本次撮合產生的成交事件（雙邊事件列表）
 * - affectedOrders：受影響的訂單集合（含新單與被部分/全成交的對手單），供上層一次回寫狀態與凍結調整
 *
 * TradeExecuted 內已攜帶 maker/taker、matchId、orderId 與 counterOrderId。
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
public class MatchingResult {
    private final List<TradeExecuted> trades;
    private final List<Order> affectedOrders;
}

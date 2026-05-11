package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.entity.Order;
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
 * TODO: 可擴充 maker/taker 角色、撮合序號（execSeq）、撮合來源（takerId/makerId）等欄位。
 */
@Getter
@ToString
@EqualsAndHashCode
@Builder
public class MatchingResult {
    private final List<TradeExecuted> trades;
    private final List<Order> affectedOrders;
}

package com.example.java21_OLAP.application.service;

import com.example.java21_OLAP.domain.model.*;
import com.example.java21_OLAP.domain.repository.*;
import com.example.java21_OLAP.domain.event.TradeExecuted;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 下單/成交相關服務（簡化示範）
 * - 真實專案應該把撮合交給撮合引擎，這裡僅示範生成成交事件並作用在 Position
 */
@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final PositionRepository posRepo;
    private final EventStore eventStore;

    public OrderService(OrderRepository orderRepo, PositionRepository posRepo, EventStore eventStore) {
        this.orderRepo = orderRepo;
        this.posRepo = posRepo;
        this.eventStore = eventStore;
    }

    /**
     * 執行「市價成交」示範流（實務上由撮合引擎產生）
     * - 將成交結果以 TradeExecuted 事件形式 append 到 EventStore
     * - 並把成交影響套用到 Position（VWAP/數量變化）
     */
    public TradeExecuted executeMarket(long uid, Symbol symbol, OrderSide side, BigDecimal qty, BigDecimal price) {
        // 簽名：BUY -> +qty；SELL -> -qty
        BigDecimal signedQty = (side == OrderSide.BUY) ? qty : qty.negate();

        Position pos = posRepo.find(uid, symbol)
                .orElseGet(() -> new Position(uid, symbol, MarginMode.CROSS, BigDecimal.valueOf(20)));

        // 作用成交到倉位（更新均價/數量）
        pos.applyTrade(signedQty, price);
        posRepo.save(pos);

        // 追加事件到 EventStore，取得遞增 seq
        TradeExecuted appended = new TradeExecuted(uid, symbol, signedQty, price, 0L, Instant.now());
        long seq = eventStore.append(appended);

        // 回傳帶 seq 的事件（方便上層/外部系統引用）
        return appended.withSeq(seq);
    }
}

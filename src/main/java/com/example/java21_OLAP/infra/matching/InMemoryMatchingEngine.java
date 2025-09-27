package com.example.java21_OLAP.infra.matching;

import com.example.java21_OLAP.domain.event.TradeExecuted;
import com.example.java21_OLAP.domain.model.*;
import com.example.java21_OLAP.domain.service.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory 撮合引擎（多交易對）
 */
@Component
public class InMemoryMatchingEngine implements MatchingEngine {

    /** key = symbolCode（例：BTCUSDT） */
    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();

    private OrderBook book(String symbolCode) {
        return books.computeIfAbsent(symbolCode, k -> new OrderBook());
    }

    @Override
    public List<TradeExecuted> submitOrder(Order order) {
        String sym = order.getSymbol().code();
        OrderBook ob = book(sym);
        List<TradeExecuted> trades = new ArrayList<>();

        synchronized (ob) {
            if (order.getSide() == OrderSide.BUY) {
                while (ob.peekBestAsk() != null
                        && order.getQty().signum() > 0
                        && order.getPrice().compareTo(ob.peekBestAsk().getPrice()) >= 0) {

                    Order bestAsk = ob.pollBestAsk();
                    BigDecimal execQty = order.getQty().min(bestAsk.getQty());
                    BigDecimal execPx  = bestAsk.getPrice();

                    trades.add(new TradeExecuted(order.getUid(),     order.getSymbol(),    execQty,         execPx, 0L, Instant.now()));
                    trades.add(new TradeExecuted(bestAsk.getUid(),   bestAsk.getSymbol(),  execQty.negate(),execPx, 0L, Instant.now()));

                    order.fill(execQty);
                    bestAsk.fill(execQty);
                    if (bestAsk.getQty().signum() > 0) ob.add(bestAsk);
                }
                if (order.getQty().signum() > 0) ob.add(order);

            } else { // SELL
                while (ob.peekBestBid() != null
                        && order.getQty().signum() > 0
                        && order.getPrice().compareTo(ob.peekBestBid().getPrice()) <= 0) {

                    Order bestBid = ob.pollBestBid();
                    BigDecimal execQty = order.getQty().min(bestBid.getQty());
                    BigDecimal execPx  = bestBid.getPrice();

                    trades.add(new TradeExecuted(order.getUid(),    order.getSymbol(),   execQty.negate(), execPx, 0L, Instant.now()));
                    trades.add(new TradeExecuted(bestBid.getUid(),  bestBid.getSymbol(), execQty,          execPx, 0L, Instant.now()));

                    order.fill(execQty);
                    bestBid.fill(execQty);
                    if (bestBid.getQty().signum() > 0) ob.add(bestBid);
                }
                if (order.getQty().signum() > 0) ob.add(order);
            }
        }

        return trades;
    }

    @Override
    public boolean cancelOrder(Order order) {
        OrderBook ob = books.get(order.getSymbol().code());
        if (ob == null) return false;
        synchronized (ob) {
            return ob.cancel(order);
        }
    }

    @Override
    public OrderBookSnapshot snapshot(String symbolCode, int depth) {
        OrderBook ob = books.get(symbolCode);
        if (ob == null) return new OrderBookSnapshot(List.of(), List.of());
        synchronized (ob) {
            return ob.snapshot(Math.max(1, depth));
        }
    }

    @Override
    public Optional<TopOfBook> top(String symbolCode) {
        OrderBook ob = books.get(symbolCode);
        if (ob == null) return Optional.empty();
        synchronized (ob) {
            var bid = (ob.peekBestBid() != null) ? ob.peekBestBid().getPrice() : null;
            var ask = (ob.peekBestAsk() != null) ? ob.peekBestAsk().getPrice() : null;
            return Optional.of(new TopOfBook(bid, ask));
        }
    }
}

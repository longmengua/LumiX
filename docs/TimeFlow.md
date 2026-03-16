## 事件時序圖：撮合 → 報表（A/B/C 三場景）

```
Actors / Lanes:
[Client]  使用者/做市商（REST/WS）
[GW]      API Gateway（AuthN/Z、限流）
[ORD]     Order App（下/撤/查、觸發單、策略單）
[RISK]    Risk-Margin（預檢、限額、MM/IM）
[WAL]     Wallet-Ledger（雙式簿記，reserve/settle）
[ME]      Matching Engine（每 symbol 單簿序列器）
[POS]     Position & PnL（均價、UPnL/RPnL、費/返）
[MD]      Market-Data（book/aggTrade/mark WS）
[PX]      Price Feed（Index/Mark，離群值剔除）
[KAFKA]   事件匯流排（order.*, trade.*, position.*, ...）
[ETL]     Stream ETL/Replay（入倉、校驗、重放）
[CH]      ClickHouse（OLAP，報表/查詢）
[ADM]     Admin/BI（報表 API/看板/告警）

──────────────────────────────────────────────────────────────────────────────
Scenario A：LIMIT/IOC → 立即全成（Taker），交易一路入報表
──────────────────────────────────────────────────────────────────────────────
[Client] -> [GW]     : POST /fapi/v1/order (symbol, side, type=LIMIT/IOC, qty, price, clientOrderId)
[GW]     -> [ORD]    : route + auth context
[ORD]    -> [RISK]   : PreCheck(uid, symbol, intent, notional, mode, leverage)
[RISK]   -> [ORD]    : OK(requiredIM, riskTier)   # 通過風控
[ORD]    -> [WAL]    : reserve(uid, asset=USDT, amount=requiredIM, ref=orderId)
[WAL]    -> [ORD]    : reserved(holdId, balanceAfter)
[ORD]    -> [ME]     : place(order)               # 進撮合序列
[ME]     -> [ME]     : price-time priority match  # 與對手方撮合
[ME]     -> [ORD]    : fillReport(orderId, trades[...], allFilled=true, fees, maker=false)
[ME]     -> [MD]     : book deltas / aggTrade     # 公有WS（行情）
[ME]     -> [KAFKA]  : emit trade.executed(matchId, taker/maker, fee, ts, schemaVersion)

# 結算/持倉
[ORD]    -> [POS]    : applyTrade(trades)         # 更新均價、UPnL/RPnL
[POS]    -> [WAL]    : settle(ledger postings: fee, realized PnL, release unused IM)
[WAL]    -> [POS]    : booked(ledgerIds...)
[POS]    -> [KAFKA]  : emit position.changed(uid, symbol, qty, entry, upnl, ts)
[ORD]    -> [Client] : REST response {orderId, status=FILLED, fills=[...]} (私有WS同步推送)
[MD]     -> [Client] : ws: @aggTrade, @depth (public)
[ORD]    -> [KAFKA]  : emit order.updated(FILLED)

# 報表鏈路
[KAFKA]  -> [ETL]    : trade.executed, position.changed, order.updated (stream)
[ETL]    -> [CH]     : insert into trades_all / positions_snapshots (w/ materialized views)
[ADM]    -> [CH]     : query: volume, fees, maker/taker ratio, user statement
[CH]     -> [ADM]    : result (BI dashboards/alerts)

──────────────────────────────────────────────────────────────────────────────
Scenario B：MARKET/部分成交 → 剩餘轉 Maker 掛簿（可選），之後再成交
──────────────────────────────────────────────────────────────────────────────
[Client] -> [GW]     : POST /fapi/v1/order (type=MARKET, qty=100)
[GW]     -> [ORD]    : route
[ORD]    -> [RISK]   : PreCheck(...)
[RISK]   -> [ORD]    : OK(requiredIM_for_worst_case)
[ORD]    -> [WAL]    : reserve(requiredIM_for_worst_case)
[ORD]    -> [ME]     : place(order=MKT)
[ME]     -> [ORD]    : partial fills: 60 filled, 40 remaining
[ME]     -> [KAFKA]  : emit trade.executed (qty=60)
[ORD]    -> [POS]    : applyTrade(60)
[POS]    -> [WAL]    : settle fee/RPnL (partial), adjust holds
[ORD]    -> [ME]     : policy: convert remaining 40 to LIMIT @ lastPrice (maker=true)  # 可配置
[ME]     -> [MD]     : book deltas (bid/ask updated)
[ORD]    -> [Client] : response {status=PARTIALLY_FILLED, executedQty=60, rest=40 on book}
...稍後市場對手出現...
[ME]     -> [ORD]    : fills for remaining 40 → allFilled
[ME]     -> [KAFKA]  : emit trade.executed (qty=40, maker=true)
[ORD]    -> [POS]    : applyTrade(40) → position/avg updated
[POS]    -> [WAL]    : settle fee/RPnL (final), release unused IM
[ORD]    -> [KAFKA]  : order.updated(FILLED)
[ETL]    -> [CH]     : all trades & position snapshots ready for reports

──────────────────────────────────────────────────────────────────────────────
Scenario C：撤單（含預扣釋放）
──────────────────────────────────────────────────────────────────────────────
[Client] -> [GW]     : DELETE /fapi/v1/order?symbol&orderId
[GW]     -> [ORD]    : route
[ORD]    -> [ME]     : cancel(orderId)
[ME]     -> [ORD]    : canceled(ok, remainingQty)
[ORD]    -> [WAL]    : release(holdId, amount=unusedIM)
[WAL]    -> [ORD]    : released(balanceAfter)
[ORD]    -> [KAFKA]  : order.canceled
[ORD]    -> [Client] : response {status=CANCELED}

──────────────────────────────────────────────────────────────────────────────
依賴：標記價/資金費觸發的風控與報表
──────────────────────────────────────────────────────────────────────────────
[PX]     -> [KAFKA]  : mark_price.updated(symbol, mark, ts)
[KAFKA]  -> [RISK]   : consume mark → recompute health → maybe trigger liquidation
[RISK]   -> [ME]     : place(liq order, IOC, protected price)
[ME]     -> [KAFKA]  : liquidation.triggered / .filled / adl.executed
[ETL]    -> [CH]     : write funding_settled / liquidation streams
[ADM]    -> [CH]     : risk board: liq success rate, ADL count, insurance fund delta

# 通知與可觀測性（橫切面）
[ORD]/[POS]/[WAL] -> [Client] : Private WS: order/account/position updates
[MD]              -> [Client] : Public WS: depth, bookTicker, aggTrade, markPrice
[ETL]/[CH]        -> [ADM]    : metrics & alerts (reporting delay, stream lag, p99 matching latency)
```

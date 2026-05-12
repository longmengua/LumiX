# poly-market

## flow

世界杯賽事 Sync Key
↓
排程 / 手動 API 觸發
↓
Gamma 全量拉 active markets
↓
用 market.events[].slug match eventSlug
↓
分類 homeWin / draw / awayWin
↓
寫入 prediction_market_info
↓
每 5 秒只刷新價格欄位
↓
GET /markets 回傳 Bitmart UI 格式

## poly market

| 模組               | 用途                                           | 你該怎麼用              |
| ---------------- | -------------------------------------------- | ------------------ |
| Gamma API        | 市場列表、賽事、問題、tokenId                           | 讀市場資料              |
| CLOB Read API    | orderbook、price、spread                       | 報價、滑點檢查            |
| CLOB Trading API | 下單、撤單、查訂單                                    | **核心下單通道**         |
| Relayer API      | gasless transaction、approval、redeem、鏈上 tx 輔助 | **不要拿來取代 CLOB 下單** |


## curls

- 手動全量發現世界杯 markets，Discovery Flow（負載：重），curl -X POST http://localhost:8080/api/prediction/markets/discover
- 用 prediction_market_sync_key 資料去讀取 market 資料，Key Sync Flow（負載：輕），curl -X POST http://localhost:8080/api/prediction/markets/sync
- 手動刷新價格，Price Refresh Flow（負載：超輕），curl -X POST http://localhost:8080/api/prediction/markets/price-refresh
- 查 key sync progress，curl http://localhost:8080/api/prediction/markets/sync-progress
- 查前端資料，curl http://localhost:8080/api/prediction/markets
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/order.sh。
# LIMIT 買單
curl -X POST http://localhost:8080/api/order/place \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 1,
    "symbol": "BTCUSDT",
    "side": "BUY",
    "type": "LIMIT",
    "price": 30000,
    "qty": 0.01,
    "leverage": 20,
    "marginMode": "CROSS"
  }'

# MARKET 賣單（price 可為 null）
curl -X POST http://localhost:8080/api/order/place \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 2,
    "symbol": "BTCUSDT",
    "side": "SELL",
    "type": "MARKET",
    "qty": 0.01,
    "leverage": 20,
    "marginMode": "ISOLATED"
  }'

# 查詢當前掛單
curl "http://localhost:8080/api/order/open?uid=1"
curl "http://localhost:8080/api/order/open?uid=1&symbol=BTCUSDT"

# 查詢所有歷史訂單
curl "http://localhost:8080/api/order/all?uid=1&symbol=BTCUSDT"

# 查 BTCUSDT 前 10 檔
curl "http://localhost:8080/api/depth/BTCUSDT?depth=10"


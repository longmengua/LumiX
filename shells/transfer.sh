# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/transfer.sh。
curl -X POST http://localhost:8080/api/margin/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 1,
    "symbol": "BTCUSDT",
    "amount": 100,
    "fromMode": "CROSS",
    "toMode": "ISOLATED"
  }'

curl -X POST http://localhost:8080/api/margin/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 1,
    "symbol": "BTCUSDT",
    "amount": 100,
    "fromMode": "CROSS",
    "toMode": "ISOLATED"
  }'

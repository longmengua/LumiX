#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/order/place" \
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
printf "\n"

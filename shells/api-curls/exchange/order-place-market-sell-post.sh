#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/order/place" \
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
printf "\n"

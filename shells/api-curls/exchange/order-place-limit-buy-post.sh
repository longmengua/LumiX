#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/exchange/order-place-limit-buy-post.sh。
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

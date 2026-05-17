#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/exchange/order-place-market-sell-post.sh。
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

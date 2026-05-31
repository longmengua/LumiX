#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/exchange/market-maker-quotes-post.sh。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/market-maker/quotes" \
  -H "Content-Type: application/json" \
  -d '{
    "marketMakerId": "mm-1",
    "uid": 1001,
    "symbol": "BTCUSDT",
    "bidPrice": 29990,
    "bidQuantity": 0.01,
    "askPrice": 30010,
    "askQuantity": 0.01,
    "refId": "quote-smoke-1"
  }'
printf "\n"

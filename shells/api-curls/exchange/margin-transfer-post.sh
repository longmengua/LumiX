#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/exchange/margin-transfer-post.sh。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/margin/transfer" \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 1,
    "symbol": "BTCUSDT",
    "toIsolated": true,
    "amount": 100
  }'
printf "\n"

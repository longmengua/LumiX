#!/usr/bin/env bash
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

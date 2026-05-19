#!/usr/bin/env bash
# File purpose: Local API curl example for PUT /api/risk/price-oracle.
set -euo pipefail

curl -sS -X PUT "http://localhost:8080/api/risk/price-oracle" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTCUSDT",
    "markPrice": 100,
    "indexPrice": 100,
    "source": "manual"
  }'
printf "\n"

#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/margin/bonus-credit/clawback.

set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/margin/bonus-credit/clawback" \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 1,
    "asset": "USDT",
    "amount": 10,
    "refId": "manual-clawback-001"
  }'

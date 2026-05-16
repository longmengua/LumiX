#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "1",
    "sessionId": "replace-with-session-id",
    "marketSlug": "fifwc-mex-rsa-2026-06-11-mex",
    "direction": "BUY_YES",
    "usdtAmount": 1,
    "orderType": "FOK"
  }'
printf "\n"

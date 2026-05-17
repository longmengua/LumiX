#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/orders-post.sh。
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

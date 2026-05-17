#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/margin/withdraw.
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/margin/withdraw" \
  -H "Content-Type: application/json" \
  -d '{
    "uid": 1,
    "amount": 100
  }'
printf "\n"

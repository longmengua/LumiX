#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/turnover/export.
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/turnover/export?uid=1&symbol=BTCUSDT&strategyId=campaign-a&marketMakerId=mm-1&limit=500"
printf "\n"

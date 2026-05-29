#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/turnover/summary.

set -euo pipefail

curl -sS "http://localhost:8080/api/margin/turnover/summary?uid=1&symbol=BTCUSDT&strategyId=campaign-a&marketMakerId=mm-1"

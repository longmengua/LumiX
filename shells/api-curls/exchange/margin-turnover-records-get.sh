#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/turnover/records.

set -euo pipefail

curl -sS "http://localhost:8080/api/margin/turnover/records?uid=1&symbol=BTCUSDT&strategyId=campaign-a&limit=50"

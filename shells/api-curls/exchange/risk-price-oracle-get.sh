#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/risk/price-oracle/{symbol}.
set -euo pipefail

curl -sS "http://localhost:8080/api/risk/price-oracle/BTCUSDT"
printf "\n"

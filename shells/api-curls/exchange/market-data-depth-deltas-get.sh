#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/market-data/{symbol}/depth-deltas.
set -euo pipefail

curl -sS "http://localhost:8080/api/market-data/BTCUSDT/depth-deltas?afterVersion=0&limit=100"
printf "\n"

#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/risk/insurance-fund/movements.
set -euo pipefail

curl -sS "http://localhost:8080/api/risk/insurance-fund/movements?asset=USDT&limit=50"

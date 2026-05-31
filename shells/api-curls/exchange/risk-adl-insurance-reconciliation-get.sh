#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/risk/adl-insurance-reconciliation.
set -euo pipefail

curl -sS "http://localhost:8080/api/risk/adl-insurance-reconciliation?asset=USDT"
printf "\n"

#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/risk/adl-queue/{liquidationId}/claim.
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/risk/adl-queue/liq-example/claim" \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "ops-1"
  }'
printf "\n"

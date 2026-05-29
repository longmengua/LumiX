#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/risk/adl-queue/{liquidationId}/release.
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/risk/adl-queue/liq-example/release" \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "ops-1"
  }'
printf "\n"

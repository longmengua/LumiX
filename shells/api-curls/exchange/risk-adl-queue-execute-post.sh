#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/risk/adl-queue/{liquidationId}/execute.
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/risk/adl-queue/liq-example/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "commandId": "adl-exec-example-1",
    "operatorId": "ops-1"
  }'
printf "\n"

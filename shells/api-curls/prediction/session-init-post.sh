#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/session/init" \
  -H "Content-Type: application/json" \
  -d '{
    "userAddress": "0x0000000000000000000000000000000000000000"
  }'
printf "\n"

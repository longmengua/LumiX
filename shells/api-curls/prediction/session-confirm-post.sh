#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/session/confirm" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "replace-with-session-id",
    "userAddress": "0x0000000000000000000000000000000000000000",
    "signature": "replace-with-personal-sign-signature"
  }'
printf "\n"

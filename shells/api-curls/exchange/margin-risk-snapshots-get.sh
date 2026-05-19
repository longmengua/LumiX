#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/risk/snapshots.
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/risk/snapshots?uid=1&limit=30"
printf "\n"

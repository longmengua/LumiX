#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/margin/risk/snapshot.
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/margin/risk/snapshot?uid=1"
printf "\n"

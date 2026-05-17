#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/risk.
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/risk?uid=1"
printf "\n"

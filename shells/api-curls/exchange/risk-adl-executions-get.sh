#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/risk/adl-executions.
set -euo pipefail

curl -sS "http://localhost:8080/api/risk/adl-executions?limit=50"
printf "\n"

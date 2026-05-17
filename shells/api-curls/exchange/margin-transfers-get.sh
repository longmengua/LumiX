#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/transfers.
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/transfers?uid=1"
printf "\n"

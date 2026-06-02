#!/usr/bin/env bash
# File purpose: curl example for the read-only admin market-config API.
set -euo pipefail

curl -sS "http://localhost:8080/api/admin/market-config"
printf "\n"

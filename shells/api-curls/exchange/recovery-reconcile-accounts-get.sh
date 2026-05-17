#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/reconcile/accounts.
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/reconcile/accounts"
printf "\n"

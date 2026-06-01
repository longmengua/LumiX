#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/restore/account-position-consistency.
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/restore/account-position-consistency"

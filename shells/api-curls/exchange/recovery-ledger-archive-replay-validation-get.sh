#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/finance/ledger-archive-replay-validation.
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/finance/ledger-archive-replay-validation?fromDate=2026-05-29&toDate=2026-05-30"

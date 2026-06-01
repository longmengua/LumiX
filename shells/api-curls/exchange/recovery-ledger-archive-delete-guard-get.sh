#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/finance/ledger-archive-delete-guard.
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/finance/ledger-archive-delete-guard?date=2026-05-30"

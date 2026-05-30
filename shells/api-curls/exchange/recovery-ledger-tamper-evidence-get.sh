#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/reconcile/ledger/tamper-evidence.

set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/reconcile/ledger/tamper-evidence"

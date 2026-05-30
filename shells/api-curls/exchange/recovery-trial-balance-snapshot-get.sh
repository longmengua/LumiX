#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/finance/trial-balance/snapshot.

set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/finance/trial-balance/snapshot?date=2026-05-30&uid=1&asset=USDT"

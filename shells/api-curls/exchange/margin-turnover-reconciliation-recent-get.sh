#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/turnover/reconciliation/recent.

set -euo pipefail

curl -sS "http://localhost:8080/api/margin/turnover/reconciliation/recent?from=2026-05-30T00:00:00Z&to=2026-05-31T00:00:00Z&limit=1000"

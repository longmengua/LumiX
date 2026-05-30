#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/turnover/reconciliation.

set -euo pipefail

curl -sS "http://localhost:8080/api/margin/turnover/reconciliation?uid=1&matchId=match-001"

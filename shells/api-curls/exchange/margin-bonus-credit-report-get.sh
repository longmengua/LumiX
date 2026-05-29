#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/bonus-credit/report.

set -euo pipefail

curl -sS "http://localhost:8080/api/margin/bonus-credit/report?uid=1&asset=USDT"

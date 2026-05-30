#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/finance/category-report.

set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/finance/category-report?date=2026-05-30&category=fee"

#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/bonus-credit/campaign-report.

set -euo pipefail

curl -sS "http://localhost:8080/api/margin/bonus-credit/campaign-report?campaignId=campaign-a&asset=USDT"

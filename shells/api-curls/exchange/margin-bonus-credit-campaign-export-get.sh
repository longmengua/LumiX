#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/margin/bonus-credit/campaign-export.
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/bonus-credit/campaign-export?campaignId=campaign-a&asset=USDT"
printf "\n"

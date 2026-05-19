#!/usr/bin/env bash
# File purpose: Local API curl example for POST /api/margin/risk/snapshots.
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/margin/risk/snapshots"
printf "\n"

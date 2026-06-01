#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/risk/adl-queue/alerts.
set -euo pipefail

curl -sS "http://localhost:8080/api/risk/adl-queue/alerts?minAgeSeconds=900"

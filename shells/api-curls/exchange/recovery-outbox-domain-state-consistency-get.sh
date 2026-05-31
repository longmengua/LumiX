#!/usr/bin/env bash
# File purpose: Local API curl example for GET /api/recovery/outbox/domain-state-consistency.
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/outbox/domain-state-consistency?limit=50"

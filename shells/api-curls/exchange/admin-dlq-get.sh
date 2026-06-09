#!/usr/bin/env bash
# File purpose: curl example for the read-only admin DLQ inspection API.
set -euo pipefail

curl -sS "http://localhost:8080/api/admin/dlq?limit=50"
printf "\n"

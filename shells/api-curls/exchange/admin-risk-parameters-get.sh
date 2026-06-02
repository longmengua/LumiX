#!/usr/bin/env bash
# File purpose: curl example for the read-only admin risk-parameters API.
set -euo pipefail

curl -sS "http://localhost:8080/api/admin/risk-parameters"
printf "\n"

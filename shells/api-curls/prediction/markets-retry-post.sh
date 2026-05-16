#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/markets/retry/fifwc-mex-rsa-2026-06-11"
printf "\n"

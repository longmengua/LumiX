#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/clob/api-key/create?nonce=0"
printf "\n"

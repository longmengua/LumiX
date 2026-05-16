#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/clob/api-key/derive?nonce=0"
printf "\n"

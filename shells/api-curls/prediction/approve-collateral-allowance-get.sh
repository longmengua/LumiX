#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/approve/collateral/allowance?owner=0x0000000000000000000000000000000000000000"
printf "\n"

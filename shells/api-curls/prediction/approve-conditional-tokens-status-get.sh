#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/approve/conditional-tokens/status?owner=0x0000000000000000000000000000000000000000"
printf "\n"

#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/session/revoke-all?userAddress=0x0000000000000000000000000000000000000000"
printf "\n"

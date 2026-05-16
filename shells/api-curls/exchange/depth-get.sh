#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/depth/BTCUSDT?depth=10"
printf "\n"

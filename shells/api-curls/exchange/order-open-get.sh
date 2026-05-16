#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/order/open?uid=1&symbol=BTCUSDT"
printf "\n"

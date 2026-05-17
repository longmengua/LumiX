#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/exchange/order-all-get.sh。
set -euo pipefail

curl -sS "http://localhost:8080/api/order/all?uid=1&symbol=BTCUSDT"
printf "\n"

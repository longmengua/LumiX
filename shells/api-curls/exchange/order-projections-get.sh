#!/usr/bin/env bash
# 檔案用途：Shell 腳本，依 uid / symbol 查詢訂單 lifecycle projection。
set -euo pipefail

curl -sS "http://localhost:8080/api/order/projections?uid=1&symbol=BTCUSDT"
printf "\n"

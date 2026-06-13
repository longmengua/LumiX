#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機查詢客戶目前持倉的 API curl 範例。
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/positions?uid=1&symbol=BTCUSDT"
printf "\n"

#!/usr/bin/env bash
# 檔案用途：Shell 腳本，從 durable wallet ledger journal replay 使用者資產狀態。
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/ledger/replay?uid=1&asset=USDT"
printf "\n"

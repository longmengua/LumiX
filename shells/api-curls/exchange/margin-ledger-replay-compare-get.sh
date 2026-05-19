#!/usr/bin/env bash
# 檔案用途：Shell 腳本，比對 durable ledger replay 與目前 account 狀態。
set -euo pipefail

curl -sS "http://localhost:8080/api/margin/ledger/replay/compare?uid=1&asset=USDT"
printf "\n"

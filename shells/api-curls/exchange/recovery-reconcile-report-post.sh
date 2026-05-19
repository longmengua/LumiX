#!/usr/bin/env bash
# 檔案用途：Shell 腳本，產生並保存全帳戶 reconciliation report。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/recovery/reconcile/accounts/report?triggeredBy=MANUAL"
printf "\n"

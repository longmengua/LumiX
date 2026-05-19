#!/usr/bin/env bash
# 檔案用途：Shell 腳本，查詢最近 persisted reconciliation reports。
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/reconcile/reports?limit=20"
printf "\n"

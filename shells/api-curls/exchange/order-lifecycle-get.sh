#!/usr/bin/env bash
# 檔案用途：Shell 腳本，查詢單筆訂單的 durable lifecycle event log。
set -euo pipefail

curl -sS "http://localhost:8080/api/order/00000000-0000-0000-0000-000000000000/lifecycle"
printf "\n"

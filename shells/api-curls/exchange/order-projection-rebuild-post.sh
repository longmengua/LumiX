#!/usr/bin/env bash
# 檔案用途：Shell 腳本，從 durable lifecycle event log 重建單筆訂單 projection。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/order/00000000-0000-0000-0000-000000000000/projection/rebuild"
printf "\n"

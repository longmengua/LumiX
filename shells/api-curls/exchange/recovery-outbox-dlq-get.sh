#!/usr/bin/env bash
# 檔案用途：Shell 腳本，查詢 durable outbox DLQ 最新事件。
set -euo pipefail

curl -sS "http://localhost:8080/api/recovery/outbox/dlq?limit=50"
printf "\n"

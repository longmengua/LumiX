#!/usr/bin/env bash
# 檔案用途：Shell 腳本，將 DEAD outbox event 標記為人工補償完成。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/recovery/outbox/dead/00000000-0000-0000-0000-000000000000/compensate?reason=manual-review"
printf "\n"

#!/usr/bin/env bash
# 檔案用途：Shell 腳本，重放 DEAD outbox event。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/recovery/outbox/dead/00000000-0000-0000-0000-000000000000/replay"
printf "\n"

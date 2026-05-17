#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/approve-conditional-tokens-status-get.sh。
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/approve/conditional-tokens/status?owner=0x0000000000000000000000000000000000000000"
printf "\n"

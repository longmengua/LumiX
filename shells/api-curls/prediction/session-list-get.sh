#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/session-list-get.sh。
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/session/list?userAddress=0x0000000000000000000000000000000000000000"
printf "\n"

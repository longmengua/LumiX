#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/clob-api-key-derive-get.sh。
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/clob/api-key/derive?nonce=0"
printf "\n"

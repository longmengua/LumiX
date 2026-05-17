#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/approve-collateral-allowance-get.sh。
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/approve/collateral/allowance?owner=0x0000000000000000000000000000000000000000"
printf "\n"

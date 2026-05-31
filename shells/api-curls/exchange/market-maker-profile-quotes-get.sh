#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/exchange/market-maker-profile-quotes-get.sh。
set -euo pipefail

curl -sS "http://localhost:8080/api/market-maker/profiles/mm-1/quotes?limit=50"
printf "\n"

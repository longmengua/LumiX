#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/markets-discover-post.sh。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/markets/discover"
printf "\n"

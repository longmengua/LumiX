#!/usr/bin/env bash
# 檔案用途：Shell 腳本，提供本機操作或 API curl 範例：shells/api-curls/prediction/ws-user-start-post.sh。
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/ws/user/start"
printf "\n"

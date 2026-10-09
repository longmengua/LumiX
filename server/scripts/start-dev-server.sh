#!/usr/bin/env bash

set -euo pipefail

# 僅供本機非正式環境使用；正式環境必須由受治理的 runtime 管理程序，不可任意終止既有服務。
if [[ "${NODE_ENV:-development}" == "production" || "${LUMIX_ENV:-development}" == "production" || "${SPRING_PROFILES_ACTIVE:-}" == *"prod"* ]]; then
  echo "拒絕執行：start-dev-server.sh 不可在 production profile 終止 8080 埠的程序。" >&2
  exit 1
fi

readonly PORT=8080
PIDS="$(lsof -tiTCP:"${PORT}" -sTCP:LISTEN 2>/dev/null || true)"

# 本機開發採固定埠；只終止確實監聽該埠的程序，避免 Spring Boot 默默改用其他埠而破壞 service boundary。
if [[ -n "${PIDS}" ]]; then
  echo "釋放本機開發埠 ${PORT}（PID: ${PIDS//$'\n'/ }）"
  while IFS= read -r PID; do
    [[ -n "${PID}" ]] && kill "${PID}"
  done <<< "${PIDS}"
  sleep 1
fi

exec ./mvnw spring-boot:run

#!/usr/bin/env bash

set -euo pipefail

# 本機依賴由 repo root 的 .env 提供；不載入它會讓 infrastructure profile 缺少 JDBC／Redis topology 設定。
readonly SERVER_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly REPOSITORY_DIRECTORY="$(cd "${SERVER_DIRECTORY}/.." && pwd)"
if [[ -f "${REPOSITORY_DIRECTORY}/.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "${REPOSITORY_DIRECTORY}/.env"
  set +a
fi

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

# 所有 JDBC adapter 與認證邊界都受 infrastructure profile 保護；本機 launcher 必須顯式啟用，避免啟動成沒有資料來源的假 runtime。
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-infrastructure}"
# Compose 將 PostgreSQL 與 Redis 綁在 loopback；只在本機 launcher 從同一份 .env 推導 application topology，缺少 DB 憑證時仍立即拒絕。
: "${POSTGRES_DB:?POSTGRES_DB is required in ../.env}"
: "${POSTGRES_USER:?POSTGRES_USER is required in ../.env}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required in ../.env}"
export LUMIX_DATABASE_PRIMARY_JDBC_URL="${LUMIX_DATABASE_PRIMARY_JDBC_URL:-jdbc:postgresql://127.0.0.1:${LUMIX_POSTGRES_PORT:-5432}/${POSTGRES_DB}}"
export LUMIX_DATABASE_PRIMARY_USERNAME="${LUMIX_DATABASE_PRIMARY_USERNAME:-${POSTGRES_USER}}"
export LUMIX_DATABASE_PRIMARY_PASSWORD="${LUMIX_DATABASE_PRIMARY_PASSWORD:-${POSTGRES_PASSWORD}}"
export LUMIX_REDIS_HOST="${LUMIX_REDIS_HOST:-127.0.0.1}"

# 不繼承使用者層的私有 mirror；此開發入口只允許公開 Maven Central，避免本機 Nexus 可用性阻斷 API 啟動。
exec ./mvnw --settings .mvn/settings-public.xml --update-snapshots spring-boot:run

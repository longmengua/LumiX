#!/usr/bin/env bash
set -euo pipefail

# 唯讀行情驗收入口：依賴在 Compose，API 與前台各自以主機 dev runtime 啟動。
# 私鑰與暫存日誌只放在 git 忽略的 target；不修改系統信任庫，也不接入資金命令。
SERVER_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPOSITORY_DIRECTORY="$(cd "${SERVER_DIRECTORY}/.." && pwd)"
PREVIEW_DIRECTORY="${SERVER_DIRECTORY}/target/market-preview"
mkdir -p "${PREVIEW_DIRECTORY}"
chmod 700 "${PREVIEW_DIRECTORY}"

if [[ "${NODE_ENV:-development}" == production || "${LUMIX_ENV:-development}" == production || "${SPRING_PROFILES_ACTIVE:-}" == *prod* ]]; then
  echo '拒絕執行：行情驗收入口僅限本機開發。' >&2
  exit 1
fi

export LUMIX_DEV_TLS_CERT="${LUMIX_DEV_TLS_CERT:-${PREVIEW_DIRECTORY}/localhost.pem}"
export LUMIX_DEV_TLS_KEY="${LUMIX_DEV_TLS_KEY:-${PREVIEW_DIRECTORY}/localhost-key.pem}"
if [[ ! -f "${LUMIX_DEV_TLS_CERT}" && ! -f "${LUMIX_DEV_TLS_KEY}" ]]; then
  # 自簽憑證僅涵蓋 localhost；瀏覽器需由使用者核對後接受，正式部署不可使用。
  openssl req -x509 -newkey rsa:2048 -nodes -sha256 -days 30 \
    -keyout "${LUMIX_DEV_TLS_KEY}" -out "${LUMIX_DEV_TLS_CERT}" \
    -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' \
    >"${PREVIEW_DIRECTORY}/certificate.log" 2>&1
  chmod 600 "${LUMIX_DEV_TLS_KEY}"
fi
[[ -f "${LUMIX_DEV_TLS_CERT}" && -f "${LUMIX_DEV_TLS_KEY}" ]] || { echo '本機 TLS 憑證或私鑰缺失。' >&2; exit 1; }

docker compose -f "${REPOSITORY_DIRECTORY}/docker-compose.yml" up -d postgres redis kafka
export LUMIX_BINANCE_MARKET_FEED_ENABLED=true
export LUMIX_BINANCE_MARKET_FEED_ALLOWED_ORIGIN=https://localhost:8088
# forwarded headers 僅由 loopback Vite 轉入；API 不可對外監聽。
export SERVER_ADDRESS=127.0.0.1
export LUMIX_ETHEREUM_OBSERVATION_ENABLED=false
export LUMIX_WITHDRAWAL_ENABLED=false
SERVER_PID=''
WEB_PID=''
cleanup() {
  # npm／Maven 會建立子程序；一起結束已啟動的程序樹，避免停止後殘留 Vite 或 Java listener。
  [[ -z "${WEB_PID}" ]] || terminate_tree "${WEB_PID}"
  [[ -z "${SERVER_PID}" ]] || terminate_tree "${SERVER_PID}"
}
terminate_tree() {
  local child
  for child in $(pgrep -P "$1" || true); do terminate_tree "${child}"; done
  kill "$1" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT TERM

(cd "${SERVER_DIRECTORY}" && exec ./scripts/start-dev-server.sh) >"${PREVIEW_DIRECTORY}/server.log" 2>&1 &
SERVER_PID=$!
READY=false
for ((attempt=0; attempt<90; attempt++)); do
  kill -0 "${SERVER_PID}" 2>/dev/null || { tail -n 50 "${PREVIEW_DIRECTORY}/server.log"; exit 1; }
  if curl --fail --silent http://127.0.0.1:8080/actuator/health > /dev/null; then READY=true; break; fi
  sleep 1
done
[[ "${READY}" == true ]] || { echo "後端未就緒，請查看 ${PREVIEW_DIRECTORY}/server.log" >&2; exit 1; }
(cd "${REPOSITORY_DIRECTORY}/web" && exec npm run dev:client) >"${PREVIEW_DIRECTORY}/web.log" 2>&1 &
WEB_PID=$!
echo '本機行情驗收：https://localhost:8088/spot/BTC-USDT（亦可使用 /spot/ETH-USDT）'
echo '首次開啟會出現本機自簽憑證提示；確認網址為 localhost 後接受。'
echo "日誌：${PREVIEW_DIRECTORY}/server.log、web.log。Ctrl-C 停止驗收程序。"
# 任一主機程序退出都停止另一個，避免留下看似可用的半套驗收入口。
while kill -0 "${SERVER_PID}" 2>/dev/null && kill -0 "${WEB_PID}" 2>/dev/null; do sleep 2; done
exit 1

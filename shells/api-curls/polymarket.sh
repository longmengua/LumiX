#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
USER_ADDRESS="${USER_ADDRESS:-0x0000000000000000000000000000000000000000}"
SESSION_ID="${SESSION_ID:-replace-with-session-id}"
SIGNATURE="${SIGNATURE:-replace-with-personal-sign-signature}"
MARKET_SLUG="${MARKET_SLUG:-fifwc-mex-rsa-2026-06-11-mex}"
DIRECTION="${DIRECTION:-BUY_YES}"
USDT_AMOUNT="${USDT_AMOUNT:-1}"
ORDER_TYPE="${ORDER_TYPE:-FOK}"
OWNER="${OWNER:-$USER_ADDRESS}"
CLOB_AUTH_NONCE="${CLOB_AUTH_NONCE:-0}"
RUN_CLOB_AUTH="${RUN_CLOB_AUTH:-0}"
RUN_SESSION="${RUN_SESSION:-0}"
RUN_APPROVAL="${RUN_APPROVAL:-0}"
RUN_USER_WS="${RUN_USER_WS:-0}"
RUN_USER_WS_STOP="${RUN_USER_WS_STOP:-0}"
RUN_REAL_ORDER="${RUN_REAL_ORDER:-0}"

json_post() {
  local path="$1"
  local payload="$2"

  curl -sS -X POST "${BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    -d "${payload}"
  printf "\n"
}

get() {
  local path="$1"

  curl -sS "${BASE_URL}${path}"
  printf "\n"
}

echo "== prediction markets =="
get "/api/prediction/markets"

echo "== create CLOB API credentials =="
if [[ "${RUN_CLOB_AUTH}" == "1" ]]; then
  json_post "/api/prediction/clob/api-key/create?nonce=${CLOB_AUTH_NONCE}" '{}'
else
  echo "Skipped. Set RUN_CLOB_AUTH=1 to create CLOB API credentials."
fi

echo "== derive CLOB API credentials =="
if [[ "${RUN_CLOB_AUTH}" == "1" ]]; then
  get "/api/prediction/clob/api-key/derive?nonce=${CLOB_AUTH_NONCE}"
else
  echo "Skipped. Set RUN_CLOB_AUTH=1 to derive CLOB API credentials."
fi

echo "== discovery: heavy =="
json_post "/api/prediction/markets/discover" '{}'

echo "== sync progress =="
get "/api/prediction/markets/sync-progress"

echo "== sync known keys =="
json_post "/api/prediction/markets/sync" '{}'

echo "== price refresh =="
json_post "/api/prediction/markets/price-refresh" '{}'

echo "== user websocket status =="
get "/api/prediction/ws/user/status"

echo "== user websocket start =="
if [[ "${RUN_USER_WS}" == "1" ]]; then
  json_post "/api/prediction/ws/user/start" '{}'
else
  echo "Skipped. Set RUN_USER_WS=1 to start Polymarket user WebSocket."
fi

echo "== user websocket stop =="
if [[ "${RUN_USER_WS_STOP}" == "1" ]]; then
  json_post "/api/prediction/ws/user/stop" '{}'
else
  echo "Skipped. Set RUN_USER_WS_STOP=1 to stop Polymarket user WebSocket."
fi

echo "== sync reset: optional heavy =="
echo "Run manually if needed:"
echo "curl -sS -X POST ${BASE_URL}/api/prediction/markets/sync-reset"

echo "== retry one event: optional =="
echo "Run manually if needed:"
echo "curl -sS -X POST ${BASE_URL}/api/prediction/markets/retry/fifwc-mex-rsa-2026-06-11"

echo "== session init =="
if [[ "${RUN_SESSION}" == "1" ]]; then
  json_post "/api/prediction/session/init" "{
    \"userAddress\": \"${USER_ADDRESS}\"
  }"
else
  echo "Skipped. Set RUN_SESSION=1 to run."
fi

echo "== session confirm =="
if [[ "${RUN_SESSION}" == "1" ]]; then
  json_post "/api/prediction/session/confirm" "{
    \"sessionId\": \"${SESSION_ID}\",
    \"userAddress\": \"${USER_ADDRESS}\",
    \"signature\": \"${SIGNATURE}\"
  }"
else
  echo "Skipped. Set RUN_SESSION=1 to run."
fi

echo "== session list =="
if [[ "${RUN_SESSION}" == "1" ]]; then
  get "/api/prediction/session/list?userAddress=${USER_ADDRESS}"
else
  echo "Skipped. Set RUN_SESSION=1 to run."
fi

echo "== session revoke =="
if [[ "${RUN_SESSION}" == "1" ]]; then
  json_post "/api/prediction/session/revoke" "{
    \"sessionId\": \"${SESSION_ID}\",
    \"userAddress\": \"${USER_ADDRESS}\"
  }"
else
  echo "Skipped. Set RUN_SESSION=1 to run."
fi

echo "== session revoke all: optional destructive =="
echo "Run manually if needed:"
echo "curl -sS -X POST '${BASE_URL}/api/prediction/session/revoke-all?userAddress=${USER_ADDRESS}'"

echo "== place polymarket order =="
if [[ "${RUN_REAL_ORDER}" == "1" ]]; then
  json_post "/api/prediction/orders" "{
    \"userId\": \"1\",
    \"sessionId\": \"${SESSION_ID}\",
    \"marketSlug\": \"${MARKET_SLUG}\",
    \"direction\": \"${DIRECTION}\",
    \"usdtAmount\": ${USDT_AMOUNT},
    \"orderType\": \"${ORDER_TYPE}\"
  }"
else
  echo "Skipped. Set RUN_REAL_ORDER=1 to place a real Polymarket order."
fi

echo "== collateral allowance =="
if [[ "${RUN_APPROVAL}" == "1" ]]; then
  get "/api/prediction/approve/collateral/allowance?owner=${OWNER}"
else
  echo "Skipped. Set RUN_APPROVAL=1 to run RPC allowance check."
fi

echo "== conditional token approval status =="
if [[ "${RUN_APPROVAL}" == "1" ]]; then
  get "/api/prediction/approve/conditional-tokens/status?owner=${OWNER}"
else
  echo "Skipped. Set RUN_APPROVAL=1 to run RPC approval check."
fi

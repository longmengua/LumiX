#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
UID="${UID:-1}"
SYMBOL="${SYMBOL:-BTCUSDT}"
DEPTH="${DEPTH:-10}"

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

echo "== limit buy order =="
json_post "/api/order/place" "{
  \"uid\": ${UID},
  \"symbol\": \"${SYMBOL}\",
  \"side\": \"BUY\",
  \"type\": \"LIMIT\",
  \"price\": 30000,
  \"qty\": 0.01,
  \"leverage\": 20,
  \"marginMode\": \"CROSS\"
}"

echo "== market sell order =="
json_post "/api/order/place" "{
  \"uid\": 2,
  \"symbol\": \"${SYMBOL}\",
  \"side\": \"SELL\",
  \"type\": \"MARKET\",
  \"qty\": 0.01,
  \"leverage\": 20,
  \"marginMode\": \"ISOLATED\"
}"

echo "== open orders =="
get "/api/order/open?uid=${UID}&symbol=${SYMBOL}"

echo "== all orders =="
get "/api/order/all?uid=${UID}&symbol=${SYMBOL}"

echo "== depth =="
get "/api/depth/${SYMBOL}?depth=${DEPTH}"

echo "== margin transfer to isolated =="
json_post "/api/margin/transfer" "{
  \"uid\": ${UID},
  \"symbol\": \"${SYMBOL}\",
  \"toIsolated\": true,
  \"amount\": 100
}"

echo "== recover snapshot =="
json_post "/api/recovery/recover/${UID}" '{}'


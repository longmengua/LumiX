#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/orders/local/replace-with-internal-order-id"
printf "\n"

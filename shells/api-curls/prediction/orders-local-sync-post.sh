#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/orders/local/replace-with-internal-order-id/sync"
printf "\n"

#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/markets/sync-progress"
printf "\n"

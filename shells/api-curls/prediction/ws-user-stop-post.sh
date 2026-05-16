#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/prediction/ws/user/stop"
printf "\n"

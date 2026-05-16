#!/usr/bin/env bash
set -euo pipefail

curl -sS "http://localhost:8080/api/prediction/ws/user/status"
printf "\n"

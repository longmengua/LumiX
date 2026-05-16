#!/usr/bin/env bash
set -euo pipefail

curl -sS -X POST "http://localhost:8080/api/recovery/recover/1"
printf "\n"

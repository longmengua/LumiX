#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${E2E_BASE_URL:-http://localhost:8080}"

if ! command -v npm >/dev/null 2>&1; then
  printf 'npm is required for browser E2E tests.\n' >&2
  exit 1
fi

if [[ "${E2E_SKIP_DOCKER:-false}" != "true" ]]; then
  docker compose up -d
fi

if [[ -f package-lock.json ]]; then
  npm ci
else
  npm install
fi

PLAYWRIGHT_BROWSERS_PATH=.playwright-browsers npx playwright install chromium

E2E_BASE_URL="$BASE_URL" npm run e2e -- "$@"

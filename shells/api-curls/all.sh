#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "## Exchange core APIs"
"${SCRIPT_DIR}/exchange-core.sh"

echo "## Polymarket APIs"
"${SCRIPT_DIR}/polymarket.sh"


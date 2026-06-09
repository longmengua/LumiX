#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOC_ROOT="$PROJECT_ROOT/doc"

mkdir -p "$PROJECT_ROOT/shells"

cp "$DOC_ROOT/AGENTS.md" "$PROJECT_ROOT/AGENTS.md"
cp "$DOC_ROOT/scripts/codex-usage.sh" "$PROJECT_ROOT/shells/codex-usage.sh"
chmod +x "$PROJECT_ROOT/shells/codex-usage.sh"

printf 'Installed Codex project rules:\n'
printf '%s\n' "- $PROJECT_ROOT/AGENTS.md"
printf '%s\n' "- $PROJECT_ROOT/shells/codex-usage.sh"

#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

section() {
  printf '\n== %s ==\n' "$1"
}

section "Git Status"
git status --short

section "Agent Entry Points"
printf 'AGENTS.md\n'
printf 'docs/ai/README.md\n'
printf 'docs/ai/code-map.md\n'
find docs/ai/maps -maxdepth 1 -type f -name '*.md' | sort
printf 'docs/en/current-state.md\n'
printf 'docs/en/todo.md\n'
printf 'docs/zh-TW/todo.md\n'
printf 'docs/tasks/README.md\n'
find docs/tasks -mindepth 2 -type f -name '*.md' | sort

section "TODO Progress"
awk '
  /^## / { section=$0 }
  /^- \[[ x]\]/ {
    total[section]++
    if ($0 ~ /^- \[x\]/) done[section]++
  }
  END {
    for (s in total) {
      printf "%s: %d/%d done\n", s, done[s]+0, total[s]
    }
  }
' docs/en/todo.md

section "Core Packages"
find src/main/java/com/example/exchange -maxdepth 2 -type d | sort

section "Focused Tests"
find src/test/java/com/example/exchange -name '*Test.java' | sort

section "Recent Java/Config Changes"
git diff --name-only -- '*.java' '*.yml' '*.sql' '*.md' | sort

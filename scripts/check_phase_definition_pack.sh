#!/bin/sh

set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

phase_files='
docs/phases/PHASE_12_DATABASE_SCHEMA.md
docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md
docs/phases/PHASE_14_BALANCE_RECONCILIATION.md
docs/phases/PHASE_15_ASSET_RESERVATION.md
docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md
docs/phases/PHASE_17_CPP_MATCHING_CORE.md
docs/phases/PHASE_18_MATCHING_INTEGRATION.md
docs/phases/PHASE_19_TRADE_SETTLEMENT.md
docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md
docs/phases/PHASE_21_DEPOSIT_SYSTEM.md
docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md
docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md
docs/phases/PHASE_24_PRODUCTION_OPEN_API.md
docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md
docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md
docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md
docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md
docs/phases/PHASE_29_POSITION_PNL_MARGIN.md
docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md
docs/phases/PHASE_31_MARGIN_LENDING.md
docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md
docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md
docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md
docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md
docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md
'

required_sections='
## Goal
## Scope
## Non-goals
## Required deliverables
## Acceptance criteria
## Required tests
## Cannot claim yet
## Codex implementation prompt
'

fail() {
  printf '%s\n' "check_phase_definition_pack.sh: $1" >&2
  exit 1
}

printf '%s\n' "Checking phase definition files..."
printf '%s' "$phase_files" | while IFS= read -r rel; do
  [ -n "$rel" ] || continue
  file="$ROOT/$rel"
  [ -f "$file" ] || fail "missing phase file: $rel"
  printf '%s' "$required_sections" | while IFS= read -r section; do
    [ -n "$section" ] || continue
    grep -q "^$section$" "$file" || fail "missing section '$section' in $rel"
  done
done

printf '%s\n' "Checking CODEX phase prompts..."
for phase in 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36; do
  grep -q "^## Phase $phase" "$ROOT/docs/CODEX_PHASE_PROMPTS.md" || fail "missing Phase $phase prompt section"
done

printf '%s\n' "Checking roadmap links..."
printf '%s' "$phase_files" | while IFS= read -r rel; do
  [ -n "$rel" ] || continue
  grep -q "$rel" "$ROOT/docs/PRODUCTION_ROADMAP.md" || fail "roadmap missing link text for $rel"
done

printf '%s\n' "Checking AI progress status..."
for phase in 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36; do
  grep -q "^| $phase | .* | planned | not_started | not_production_completed |" "$ROOT/AI_PROGRESS.md" || fail "AI_PROGRESS.md phase $phase status is not planned/not_started"
done

printf '%s\n' "Checking production-claim guardrails..."
grep -q "does not currently have production trading" "$ROOT/README.md" || fail "README.md missing explicit no-production-trading statement"
grep -q "Do not claim production trading" "$ROOT/server/README.md" || fail "server/README.md missing explicit no-production-trading statement"
if grep -qi "production trading completed" "$ROOT/README.md" "$ROOT/server/README.md"; then
  fail "README or server README claims production trading completed"
fi

printf '%s\n' "Phase definition pack checks passed."

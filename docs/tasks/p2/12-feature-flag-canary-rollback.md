# Task: Feature Flag Canary Rollback

Status: `todo`

## Goal

Add rollout controls and documentation for feature flags, canary enablement, rollback, and schema compatibility checks.

## Scope

- Inventory feature flags and runtime switches for exchange, Polymarket, market-maker, and reporting features.
- Define canary eligibility, rollout percentages or cohorts, and rollback triggers.
- Document schema backward-compatibility expectations for migrations and API DTOs.
- Add operator runbook for enable, monitor, rollback, and post-rollback verification.

## First Implementation Slice

1. Inventory existing config switches and kill switches.
2. Add a rollout matrix doc or service baseline for flag metadata.
3. Define rollback smoke checks for core order flow, market data, ledger, and Polymarket flows.
4. Add tests for flag metadata parsing if code is introduced.

## Acceptance Criteria

- Operators can see which features are flag-controlled and how rollback works.
- Canary rollout has explicit health signals and stop conditions.
- Schema compatibility rules are documented for forward and backward deployment order.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/polymarket-security.md](../../ai/maps/polymarket-security.md)


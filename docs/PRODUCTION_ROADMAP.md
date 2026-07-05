# Production Roadmap

This document is the authoritative post-Phase-11 roadmap for LumiX.

## Current Authoritative Status

- Phase 11 is completed as a production architecture reset and documentation phase only.
- Phase 12 through Phase 36 are planned and not started.
- LumiX cannot currently claim production trading completed.
- LumiX cannot currently claim production launch ready.
- `docs/phases/` contains the per-phase definition pack for Phase 12 through Phase 36.

## Roadmap Groups

- Phase 12-15 are funds safety and ledger foundation.
- Phase 16-20 are spot trading core.
- Phase 21-23 are wallet and treasury.
- Phase 24-27 are API, admin, risk, and liquidity operations.
- Phase 28-31 are futures and margin expansion.
- Phase 32-36 are reconciliation, security, infra, and launch readiness.

## Status Key

- `completed_docs_only`: completed as documentation or architecture reset only
- `planned`: defined but not started
- `not_started`: no runtime implementation has begun

## Phase 11

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 11 | completed_docs_only | Production architecture reset, audit, safety model, and roadmap reset | Phase 10 audit baseline | High | No direct touch | Mandatory human review | [docs/PHASE_11_CHECKLIST.md](PHASE_11_CHECKLIST.md) |

## Phase 12-15: Funds Safety and Ledger Foundation

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 12 | planned / not_started | Define production schema and migration chain for balances, journals, reservations, orders, wallet, reconciliation, and audit | Phase 11 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_12_DATABASE_SCHEMA.md](phases/PHASE_12_DATABASE_SCHEMA.md) |
| 13 | planned / not_started | Implement immutable double-entry ledger posting, idempotency, reversal model, and audit trail | Phase 12 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md](phases/PHASE_13_DOUBLE_ENTRY_LEDGER.md) |
| 14 | planned / not_started | Build balance projection, rebuild tooling, ledger-vs-balance reconciliation, and audit reports | Phase 12-13 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_14_BALANCE_RECONCILIATION.md](phases/PHASE_14_BALANCE_RECONCILIATION.md) |
| 15 | planned / not_started | Implement reserve, release, commit, rollback, partial fill support, and stuck reservation detection | Phase 12-14 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_15_ASSET_RESERVATION.md](phases/PHASE_15_ASSET_RESERVATION.md) |

## Phase 16-20: Spot Trading Core

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 16 | planned / not_started | Implement production Java spot order orchestration with validation, reserve, persist, submit, cancel, and query APIs | Phase 12-15 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_16_PRODUCTION_SPOT_ORDER.md](phases/PHASE_16_PRODUCTION_SPOT_ORDER.md) |
| 17 | planned / not_started | Implement deterministic C++ matching engine, authoritative order book, replay, snapshot, and recovery | Phase 11 boundary, Phase 16 integration target | Critical | Yes | Mandatory human review | [docs/phases/PHASE_17_CPP_MATCHING_CORE.md](phases/PHASE_17_CPP_MATCHING_CORE.md) |
| 18 | planned / not_started | Implement Java-to-C++ command/event protocol, sequence safety, replay, backpressure, and circuit breaker | Phase 16-17 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_18_MATCHING_INTEGRATION.md](phases/PHASE_18_MATCHING_INTEGRATION.md) |
| 19 | planned / not_started | Implement fill-driven trade settlement, fee calculation, reserve commit, unused reserve release, and compensation path | Phase 13-18 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_19_TRADE_SETTLEMENT.md](phases/PHASE_19_TRADE_SETTLEMENT.md) |
| 20 | planned / not_started | Build production market-data pipeline with snapshots, deltas, tape, ticker, kline, Redis, REST, and WebSocket | Phase 17-19 | High | No direct mutation | Mandatory human review | [docs/phases/PHASE_20_MARKET_DATA_PIPELINE.md](phases/PHASE_20_MARKET_DATA_PIPELINE.md) |

## Phase 21-23: Wallet and Treasury

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 21 | planned / not_started | Implement deposit address, chain observation boundary, confirmation policy, reorg handling, and idempotent credit | Phase 12-14 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_21_DEPOSIT_SYSTEM.md](phases/PHASE_21_DEPOSIT_SYSTEM.md) |
| 22 | planned / not_started | Implement withdrawal request, reserve, approval, risk review, whitelist, fee deduction, broadcast boundary, and failed release | Phase 12-15, 21 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_22_WITHDRAWAL_SYSTEM.md](phases/PHASE_22_WITHDRAWAL_SYSTEM.md) |
| 23 | planned / not_started | Implement hot/cold wallet treasury, thresholds, batching, signer boundary, treasury reconciliation, and alerts | Phase 21-22 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md](phases/PHASE_23_HOT_COLD_WALLET_TREASURY.md) |

## Phase 24-27: API / Admin / Risk / Liquidity Operations

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 24 | planned / not_started | Implement production Open API with key management, signature, whitelist, rate limit, and restricted withdraw routes | Phase 16, 20, wallet prerequisites where applicable | High | Yes | Mandatory human review | [docs/phases/PHASE_24_PRODUCTION_OPEN_API.md](phases/PHASE_24_PRODUCTION_OPEN_API.md) |
| 25 | planned / not_started | Implement admin RBAC, lookup flows, review queues, adjustment requests, four-eyes approval, and audit logs | Phase 21-24 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_25_ADMIN_BACK_OFFICE.md](phases/PHASE_25_ADMIN_BACK_OFFICE.md) |
| 26 | planned / not_started | Implement user, symbol, and global risk controls plus halt, pause, and kill-switch behavior | Phase 16, 20, 22, 25 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md](phases/PHASE_26_RISK_ENGINE_KILL_SWITCH.md) |
| 27 | planned / not_started | Implement market-maker permissions, quote limits, STP, wash-trading detection, fee tiers, and liquidity monitoring | Phase 24-26 | High | Yes | Mandatory human review | [docs/phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md](phases/PHASE_27_MARKET_MAKER_LIQUIDITY.md) |

## Phase 28-31: Futures / Margin Expansion

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 28 | planned / not_started | Define futures contracts, tick/lot size, funding interval, price boundaries, leverage, margin mode, and risk tiers | Phase 20, 24, 26 | High | Yes | Mandatory human review | [docs/phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md](phases/PHASE_28_FUTURES_CONTRACT_FOUNDATION.md) |
| 29 | planned / not_started | Implement position lifecycle, realized/unrealized PnL, margin modes, leverage adjustment, and funding settlement | Phase 13, 14, 19, 28 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_29_POSITION_PNL_MARGIN.md](phases/PHASE_29_POSITION_PNL_MARGIN.md) |
| 30 | planned / not_started | Implement liquidation triggers, partial liquidation, bankruptcy handling, insurance fund, ADL, and chaos-tested auditability | Phase 28-29 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md](phases/PHASE_30_LIQUIDATION_ADL_INSURANCE.md) |
| 31 | planned / not_started | Implement margin lending with borrow, repay, interest, collateral valuation, forced repayment, and bad debt handling | Phase 13-15, 26 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_31_MARGIN_LENDING.md](phases/PHASE_31_MARGIN_LENDING.md) |

## Phase 32-36: Reconciliation / Security / Infra / Launch Readiness

| Phase | Status | Short summary | Dependency | Risk | User funds impact | Review gate | Definition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 32 | planned / not_started | Implement cross-domain reconciliation, stuck-state detection, and compensation workflow with approval gates | Phase 14, 19-22 and live-product prerequisites | Critical | Yes | Mandatory human review | [docs/phases/PHASE_32_RECONCILIATION_COMPENSATION.md](phases/PHASE_32_RECONCILIATION_COMPENSATION.md) |
| 33 | planned / not_started | Harden secrets, abuse detection, compliance hooks, anomaly alerts, dependency audit, and pen-test remediation | Phase 24-26, 32 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md](phases/PHASE_33_SECURITY_COMPLIANCE_HARDENING.md) |
| 34 | planned / not_started | Implement structured logs, metrics, tracing, dashboards, alerts, runbooks, severity policy, and postmortem tooling | Phase 19-22, 32-33 | High | No direct mutation | Mandatory human review | [docs/phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md](phases/PHASE_34_OBSERVABILITY_SRE_INCIDENT.md) |
| 35 | planned / not_started | Implement production build, deployment, rollback, backup, restore, and disaster-recovery release controls | Phase 12-34 as applicable | High | Yes | Mandatory human review | [docs/phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md](phases/PHASE_35_PRODUCTION_INFRA_CICD_RELEASE.md) |
| 36 | planned / not_started | Complete pre-launch certification, legal/support readiness, launch rehearsal, and explicit go/no-go review | Phase 12-35 | Critical | Yes | Mandatory human review | [docs/phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md](phases/PHASE_36_PRE_LAUNCH_CERTIFICATION.md) |

## Important Roadmap Rules

- Do not jump phases.
- Do not treat interface, stub, mock, placeholder, or TODO work as production completed.
- Do not claim production trading completed until the relevant readiness gates have passed.
- Do not claim production launch ready before Phase 36 passes with explicit human sign-off.

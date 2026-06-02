# Task: Matching TPS Load Test

Status: `todo`

## Goal

Add a repeatable load test spec and tool entry point for matching-engine command throughput and deterministic replay pressure.

## Scope

- Drive synthetic command streams through matching worker or in-memory engine test harness.
- Measure command throughput, match latency, snapshot/log overhead, and replay validation time.
- Cover multiple symbols and interleaved offsets when supported by the harness.
- Avoid modifying production data stores unless an explicit isolated profile is configured.

## First Implementation Slice

1. Identify the safest matching harness entry point.
2. Add load profile configuration for symbols, order count, cancel ratio, and seed.
3. Add report output for TPS, latency percentiles, fills, rejects, and replay duration.
4. Add deterministic seed support so runs can be compared.
5. Add a lightweight smoke check for profile parsing.

## Acceptance Criteria

- Runs are deterministic for a fixed seed and profile.
- Report distinguishes matching latency from replay validation time.
- Multi-symbol load is represented or explicitly documented as pending.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../core-kernel/01-replayable-matching-core.md](../core-kernel/01-replayable-matching-core.md)


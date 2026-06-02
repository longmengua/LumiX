# Task: Order Entry TPS Load Test

Status: `todo`

## Goal

Add a repeatable load test spec and tool entry point for order-entry throughput, rejection behavior, and API latency.

## Scope

- Submit synthetic limit/market/cancel commands against a non-production environment.
- Capture throughput, latency percentiles, rejection rate, cancel rate, and error categories.
- Include environment guardrails to prevent accidental production execution.
- Produce a compact report artifact for comparison across runs.

## First Implementation Slice

1. Choose the project-local load-test location and runtime.
2. Add a disabled-by-default script or test profile with required environment variables.
3. Implement a dry-run mode that validates config without sending orders.
4. Add sample report format and README instructions.
5. Add a smoke test for config parsing if practical.

## Acceptance Criteria

- Load test cannot run without explicit non-production target configuration.
- Report includes TPS, p50/p95/p99 latency, rejections, and error buckets.
- Dry-run mode is documented and safe by default.

## Read First

- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/web-apps.md](../../ai/maps/web-apps.md)


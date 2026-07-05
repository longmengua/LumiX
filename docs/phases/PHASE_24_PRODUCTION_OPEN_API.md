# Phase 24 - Production Open API

## Phase status

- Planned
- Not started
- Not production completed

## Goal

Implement the production Open API layer for market, account, order, and restricted withdrawal endpoints with strong authentication and audit controls.

## Why this phase exists

External traders, market makers, and automation clients need a real API, but production exposure requires strong key management, signing, nonce or timestamp controls, IP policy, and rate limiting.

## Dependencies

- Previous phases required: Phase 16, Phase 18, Phase 20, and wallet phases as needed for restricted wallet endpoints
- External dependencies if any: signature standard choice, rate-limit storage, API documentation tooling
- Blocking risks: signature bypass, weak permission scopes, withdraw misuse, insufficient audit logs

## Scope

- API key management
- Permission scopes
- HMAC / RSA signature
- Timestamp / nonce
- IP whitelist
- Rate limit
- Order API
- Account API
- Market API
- Withdraw API restrictions
- API audit logs

## Non-goals

- Unrestricted withdrawal API enablement
- Futures or margin APIs before their product engines are ready
- Admin API replacement

## Required deliverables

- Production API key lifecycle
- Signature verification implementation
- Timestamp and nonce enforcement
- IP whitelist controls
- Rate-limit enforcement
- Order/account/market API exposure
- Restricted withdrawal API rules
- API audit logging
- Open API test suite

## Acceptance criteria

- Signatures validate correctly for supported modes
- Permission scopes are enforced per route
- Rate limits do not fail open
- Withdrawal API stays restricted by policy
- Audit logs capture key, route, actor, and outcome

## Required tests

- API key lifecycle tests
- HMAC or RSA signature tests
- Timestamp and nonce tests
- IP whitelist tests
- Rate-limit tests
- Permission-scope tests
- Restricted withdrawal API tests

## Files / modules likely affected

- `server/src/main/java/com/lumix/openapi/`
- security/auth packages
- API controllers
- cache integration for rate limits

## Data model impact

- May add key metadata, nonce tracking, IP whitelist records, and API audit tables if not already covered

## API impact

- Introduces production Open API surface for external clients
- Withdraw APIs remain tightly restricted and policy-gated

## Security impact

- High
- Requires careful secret handling, scope enforcement, replay prevention, and audit logging

## User funds impact

- Yes
- Review requirements: mandatory human review before merge because trading and wallet APIs can expose user funds through automation

## Risk level

- High

## Review gate

- Mandatory human review before merge: Yes
- Why: authentication and permission defects can become direct fund-loss paths

## Cannot claim yet

- admin back-office completed
- full risk controls completed
- market maker operational controls completed
- launch readiness completed

## Next phase handoff

Phase 25 adds admin RBAC, lookup flows, review queues, approval controls, and audit logging for operations staff.

## Codex implementation prompt

```text
Reload the repo from disk before working. Read AI_PROGRESS.md, README.md, server/README.md, docs/PRODUCTION_ROADMAP.md, docs/PHASE_DEPENDENCY_MAP.md, docs/PRODUCTION_READINESS_GATES.md, docs/TRADING_CORE_BOUNDARIES.md, and docs/phases/PHASE_24_PRODUCTION_OPEN_API.md.

Goal: implement Phase 24 only - Production Open API.
Scope: API key management, permission scopes, HMAC/RSA signature, timestamp/nonce, IP whitelist, rate limit, order/account/market APIs, restricted withdraw APIs, and API audit logs.
Non-goals: unrestricted withdraw API enablement, unfinished futures or margin APIs, later phases.
Deliverables: production Open API layer, tests, and progress/doc updates tied to real implementation.
Tests: key lifecycle, signature, timestamp/nonce, IP whitelist, rate limit, scope enforcement, restricted withdraw API, and build validation.
Docs to update: AI_PROGRESS.md and the Phase 24 doc only if implementation changes reality.
Validation commands: cd server && ./mvnw test && ./mvnw package; cd ../web && npm install && npm run build; run npm test only if a test script exists.
Cannot claim yet: admin back-office completed, full risk controls completed, market maker operational controls completed, launch readiness completed.
Final output format: Changed Files, Summary, What Phase 24 completed, What is still NOT completed, Validation Results, Next Recommended Command.
```

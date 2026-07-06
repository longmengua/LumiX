# ADR-0001 Production Exchange Boundaries

## 決定

LumiX separates application services from exchange-core rules. Controllers must not mutate funds directly.

## Reason

Trading and wallet flows need auditability, deterministic state transitions, and human review around funds safety.

## 後果

- More files and explicit boundaries.
- Less accidental money mutation.
- Easier AI task routing.

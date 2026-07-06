# Data Flow

## Command flow

```text
Client command
  -> API validation
  -> authorization
  -> idempotency check
  -> application service
  -> domain rule
  -> database transaction
  -> outbox event
  -> worker delivery
```

## Query flow

```text
Client query
  -> API validation
  -> authorization
  -> read model / projection
  -> response
```

## Source of truth rule

```text
Ledger journal     = truth for funds
Balance projection = derived cache for query speed
Order table        = truth for order lifecycle
Trade table        = truth for executed matches
Outbox table       = truth for pending side effects
Audit log          = truth for privileged human/system actions
```

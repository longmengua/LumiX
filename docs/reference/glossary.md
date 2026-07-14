# 詞彙表

```text
Ledger
  Immutable source of truth for assets and liabilities.

Balance projection
  Query-optimized current balance derived from ledger entries.

Reservation
  A hold on available balance before order execution or withdrawal processing.

Settlement
  The atomic posting that turns matched trades into ledger entries.

Outbox
  Durable table for side effects that must be delivered after database commit.

Idempotency key
  Client or system supplied key preventing duplicate effects from retry.

Confirmation policy
  Rule that defines when a chain deposit is safe to credit.

Hot wallet
  Online signing wallet with strict limits and monitoring.

Cold wallet
  Offline or heavily controlled wallet for treasury custody.
```

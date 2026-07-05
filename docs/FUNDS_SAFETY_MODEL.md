# Funds Safety Model

This document defines the production balance and reservation model that LumiX must satisfy before live trading or live wallet movement can be claimed.

It is a Phase 11 architecture contract only. It is not yet implemented.

## Canonical Balance Definitions

- `total balance`: confirmed funds owned by the user inside the exchange ledger domain.
- `locked balance`: funds reserved for open obligations such as orders or pending withdrawals.
- `available balance`: funds that remain spendable after reservations.
- `frozen balance`: UI or operational synonym for `locked balance`; production logic should use one canonical term consistently.

Required invariant:

```text
total balance = available balance + locked balance
```

No service may write values that violate this invariant.

## Reservation Operations

### reserve

- Purpose: move funds from `available` to `locked`.
- Preconditions:
  - user account exists
  - asset is supported in that account domain
  - `available >= reserve_amount`
  - request is idempotent by reservation identifier
- Postconditions:
  - available decreases by `reserve_amount`
  - locked increases by `reserve_amount`
  - reservation state becomes active

### release

- Purpose: return unused reserved funds from `locked` back to `available`.
- Preconditions:
  - reservation exists
  - release amount does not exceed remaining locked amount
  - request is idempotent
- Postconditions:
  - locked decreases by release amount
  - available increases by release amount

### commit

- Purpose: consume reserved funds into their final journaled business effect.
- Preconditions:
  - reservation exists and is active
  - commit amount does not exceed remaining locked amount
  - downstream business event is authoritative, such as a fill or an approved withdrawal completion
- Postconditions:
  - locked decreases by commit amount
  - ledger journals record the final asset movement
  - remaining reservation stays active or closes when fully consumed

### rollback

- Purpose: revert a reservation-related flow when no irreversible external effect has been finalized.
- Preconditions:
  - failure is known and the flow is eligible for rollback
  - request is idempotent
- Postconditions:
  - remaining locked funds are restored safely
  - rollback event is auditable
  - the failed business flow remains traceable

## Idempotency Requirements

- Every reserve, release, commit, and rollback operation must be idempotent.
- Recommended keys:
  - `reservation_id` for the lifecycle container
  - `request_id` for the initiating business request
  - `event_id` for fill-driven settlement
  - `tx_hash` for blockchain-driven wallet state
- Duplicate requests must return the same durable outcome instead of applying the mutation twice.

## Negative Balance Prevention

Production implementation must prevent negative available or locked balances through layered controls:

- precondition checks before mutation
- durable concurrency control such as row-level locking or equivalent serialization
- database constraints where possible
- no blind decrement operations
- no direct balance edits outside ledger and reservation boundaries

If a mutation cannot satisfy the precondition, it must fail closed.

## Partial Fill Handling

Partial fills must not settle the entire reservation at once.

Buy order example:

- reserve the maximum quote-side requirement plus defined fee policy buffer
- on each fill, commit only the actual fill notional plus fee
- keep the remaining quote reserve locked for the remaining open quantity
- when the order finishes or is cancelled, release any unused quote reserve

Sell order example:

- reserve the base quantity being offered
- on each fill, commit only the executed base quantity
- credit quote proceeds through settlement journals
- release any remaining base reserve when the order reaches terminal state

## Cancel Order Handling

- A cancel request is not allowed to release funds immediately on user request alone.
- Release requires authoritative knowledge of the remaining open quantity from the matching core.
- If cancel acknowledgement is delayed or uncertain, the reservation must remain locked until the authoritative state is known or the case is escalated.

## Failed Settlement Handling

Settlement failure is a protected error state, not permission for silent edits.

Required behavior:

- record the fill event as received
- mark settlement status as failed or retry-pending
- do not double-commit or double-release during retries
- preserve audit links between order, fill, reservation, and journal state
- use controlled compensation only through reviewed ledger workflows

If settlement fails after the matching core has emitted an authoritative fill:

- user funds must remain in a safe, explainable state
- retries must be deterministic
- any compensation must be journaled and reviewed

## Domain-Specific Examples

Spot trading:

- reserve before order submission
- commit per fill
- release on cancel, reject, expiry, or fully settled residual

Withdrawal:

- reserve before approval or broadcast
- commit only after irreversible outbound success conditions are met by policy
- rollback or release when the withdrawal fails safely before finalization

Deposit:

- no reserve needed
- credit only after confirmation and policy checks

## Phase 11 Conclusion

LumiX cannot claim production fund freeze or production balance safety until all of the following exist together:

- double-entry ledger mutation
- balance projection
- reservation lifecycle implementation
- idempotent commit/release/rollback behavior
- deterministic settlement integration

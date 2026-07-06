# UX Safety

## Order form safety

```text
User input -> local validation -> server preview optional -> submit -> server response
```

Frontend must not claim final execution before backend confirms order/trade state.

## Wallet safety

- Deposit address must show network clearly.
- Withdrawal must show fee, receive amount, and risk status.
- Pending states must be visible.
- Failed withdrawals must show support-safe reason, not internal secrets.

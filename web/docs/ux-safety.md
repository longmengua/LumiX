# UX Safety

## 訂單 form safety

```text
User input -> local validation -> server preview optional -> submit -> server response
```

Frontend must not claim final execution before backend confirms order/trade state.

## Wallet safety

- 入金 address must show network clearly.
- 提款al must show fee, receive amount, and risk status.
- Pending states must be visible.
- Failed withdrawals must show support-safe 原因, not internal secrets.

# 錢包邊界

Wallet flows have stricter safety than ordinary trading APIs.

## 入金 boundary

```text
Chain transaction observed
  -> confirmation policy
  -> duplicate check
  -> credit command
  -> ledger entry
  -> balance projection
```

## 提款al boundary

```text
Withdrawal request
  -> validate address / network / amount
  -> reserve funds
  -> risk review
  -> approval
  -> signing request
  -> broadcast
  -> chain confirmation
  -> final reconciliation
```

## Signing boundary

```text
API service  --request-->  Signing service / custody boundary
API service  <--result---  signature / tx id / failure
```

Private keys must never be accessible to normal API controllers.

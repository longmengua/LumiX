# 帳本不變式

## Core rule

帳本 is immutable and append-only.

```text
No update to historical ledger entries
No delete of historical ledger entries
Correction = new reversal / adjustment entry
```

## Double-entry invariant

For every journal:

```text
sum(debits by asset) == sum(credits by asset)
```

Example:

```text
Journal: trade settlement
  debit  user_buyer_quote      100.00 USDT
  credit user_seller_quote      99.90 USDT
  credit exchange_fee_revenue    0.10 USDT
```

## Account types

```text
USER_ASSET
EXCHANGE_REVENUE
EXCHANGE_LIABILITY
CHAIN_PENDING
WITHDRAWAL_PENDING
FEE_RECEIVABLE
ADJUSTMENT
```

## 餘額投影 invariant

```text
balance_projection(account, asset)
  = sum(ledger_entries for account and asset)
```

餘額投影 may be cached, but must be rebuildable from ledger.

## Prohibited shortcuts

- Directly increasing balance without ledger entry.
- Directly decreasing balance without ledger entry.
- 把訂單表當成資金真相。
- Using Redis as fund truth.
- Using admin tool to mutate current balance without audit and ledger adjustment.

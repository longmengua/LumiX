# Reconciliation

## Reconciliation layers

```text
Ledger vs balance projection
Ledger vs order/trade settlement
Wallet ledger vs chain balance
Deposit records vs chain transactions
Withdrawal records vs chain transactions
Revenue ledger vs fee reports
```

## Rebuild flow

```text
ledger entries
  -> group by account/asset
  -> rebuild balance projection
  -> compare current projection
  -> report diff
  -> human-reviewed repair if needed
```

## Repair rule

No direct historical mutation. Repair requires new adjustment journal plus audit log.

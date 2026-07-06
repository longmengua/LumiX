# 手續費與收益政策

## Fee model

```text
Trade value = executed_quantity * executed_price
Fee amount  = trade_value * fee_rate
Fee asset   = quote asset by default unless market policy says otherwise
```

## Fee posting

```text
Buyer quote account  -> debit trade cost + fee
Seller base account  -> debit sold base
Seller quote account -> credit trade proceeds minus fee if seller fee in quote
Exchange revenue     -> credit fee
```

## Fee rounding

Fee rounding is money-影響ing. Any 實作 must define：

- 精度 per asset。
- rounding mode。
- minimum fee。
- zero-fee promotional policy。
- maker / taker distinction。
- audit report format。

## Human review

Fee policy changes always require human review because they directly affect revenue and user balances.

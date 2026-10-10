# 後端 Module Map

```text
com.lumix
  auth
  account
  asset
  ledger
  balance
  reservation
  order
  matching
  settlement
  fee
  wallet
  risk
  admin
  audit
  outbox
  shared
```

## 相依 direction

```text
controller -> application service -> domain service -> repository
                                      |
                                      v
                                  exchange-core
```

Forbidden:

```text
controller -> repository for money mutation
controller -> ledger table directly
wallet -> order internals
frontend DTO -> database entity reuse
```

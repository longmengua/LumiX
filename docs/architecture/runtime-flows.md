# 執行期流程

## 入金 flow

```text
User              Chain Listener       Wallet Service       Ledger        Balance
 |                      |                    |                 |             |
 |-- deposit address -->|                    |                 |             |
 |                      |-- tx observed ---->|                 |             |
 |                      |-- confirmations -->|                 |             |
 |                      |                    |-- credit cmd -->|             |
 |                      |                    |                 |-- entry ---->|
 |                      |                    |                 |             |
 |<--------------------- balance updated after credit ------------------------|
```

入金 observed is not deposit credited. Credit happens only after 確認政策 passes.

## Spot order flow

```text
User/API        Order Service      Reservation       Matching       Settlement       Ledger
  |                  |                  |                |               |              |
  |-- place order -->|                  |                |               |              |
  |                  |-- reserve funds ->|                |               |              |
  |                  |<-- reservation id-|                |               |              |
  |                  |-- send order --------------------->|               |              |
  |                  |                  |                |-- match ------>|              |
  |                  |                  |                |               |-- entries -->|
  |<---------------- accepted / filled / partially filled ---------------------|
```

## Cancel order flow

```text
User/API       Order Service       Matching       Reservation       Ledger
  |                 |                 |                |              |
  |-- cancel ------>|                 |                |              |
  |                 |-- cancel ------>|                |              |
  |                 |<-- result ------|                |              |
  |                 |-- release remaining hold --------->|              |
  |<---------------- final order state ---------------------------------------|
```

## 提款al flow

```text
User/API       Wallet Service       Risk/Admin       Reservation       Signer       Chain
  |                 |                  |                 |              |           |
  |-- request ----->|                  |                 |              |           |
  |                 |-- reserve funds ->|                 |              |           |
  |                 |-- risk review --------------------->|              |           |
  |                 |<---------------- approved / rejected|              |           |
  |                 |-- sign request ---------------------------------->|           |
  |                 |                                      |-- broadcast --------->|
  |<---------------- status: requested/approved/sent/confirmed/failed --------|
```

提款al requested is not withdrawal paid. Signing and broadcast require stronger controls than ordinary API calls.

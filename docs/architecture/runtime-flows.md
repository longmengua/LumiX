# Runtime Flows

## Deposit flow

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

Deposit observed is not deposit credited. Credit happens only after confirmation policy passes.

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

## Withdrawal flow

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

Withdrawal requested is not withdrawal paid. Signing and broadcast require stronger controls than ordinary API calls.

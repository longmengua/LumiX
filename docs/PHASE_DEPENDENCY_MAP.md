# Phase Dependency Map

## Dependency graph

```text
P12 Database
  |
  +--> P13 Identity / Account / Asset
          |
          +--> P14 Ledger
                  |
                  +--> P15 Balance Projection / Reconciliation
                          |
                          +--> P16 Reservation
                                  |
                                  +--> P17 Order Intake
                                          |
                                          +--> P18 Matching Contract
                                                  |
                                                  +--> P19 Settlement
                                                          |
                                                          +--> P20 Fee Engine

P12 Database
  |
  +--> P22 Deposit Schema / Chain Listener
          |
          +--> P23 Deposit Crediting

P12 Database
  |
  +--> P24 Withdrawal Request
          |
          +--> P25 Approval / Signing / Broadcast
```

## Review implication

若 Phase 12 schema 設計不穩，後續帳本、凍結、撮合、結算、錢包都會返工。因此 Phase 12 必須先以 production data model 為中心，而不是以畫面或 controller 為中心。

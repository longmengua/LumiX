# 階段依賴圖

## 相依圖

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

P20 Contract Trading Integration Gate
  |
  +--> P21 Market Data Pipeline
          |
          +--> P26 Risk Control & Limits
                  |
                  +--> P27 Admin Console Foundation
                          |
                          +--> P28 Audit, Compliance, Evidence Export
                                  |
                                  +--> P29 Public / Private API Hardening
                                          |
                                          +--> P30 前端正式交易 UX
                                                  |
                                                  +--> P31 Observability & Alerting
                                                          |
                                                          +--> P32 Disaster Recovery & Replay
                                                                  |
                                                                  +--> P33 Security Hardening
                                                                          |
                                                                          +--> P34 Load / Soak / Chaos Testing
                                                                                  |
                                                                                  +--> P35 Business Operations Readiness
                                                                                          |
                                                                                          +--> P36 正式上線門檻

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

P22 Deposit Address / Chain Listener
  |
  +--> P23 Deposit Crediting & Confirmation Policy
          |
          +--> P28 Audit, Compliance, Evidence Export

P24 Withdrawal Request Workflow
  |
  +--> P25 Withdrawal Approval / Signing / Broadcast
          |
          +--> P28 Audit, Compliance, Evidence Export
                  |
                  +--> P31-P36 launch gates
```

## Review implication

若 第 12 階段 schema 設計不穩，後續帳本、凍結、撮合、結算、錢包都會返工。因此 第 12 階段 必須先以 production data model 為中心，而不是以畫面或 controller 為中心。

## 加速軌限制

P20 後可用波次方式加速 task card、測試 fixture、資料契約與 review checklist 的準備；但依賴圖上的 runtime 實作不得跳過相依 phase 或未批准的 review。

特別是 P16-P20 已完成的 sandbox foundation 仍需 production closure：matching/fill、position/margin、fee/funding、ledger/balance/reservation 與 settlement/reconciliation 都必須在後續明確 task 中完成並重新人審。這些缺口不是可由 P20 final review 自動消除的相依關係。

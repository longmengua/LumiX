# Production Roadmap

## Roadmap by capability

```text
Foundation
  P12 schema
  P13 identity / account / asset
  P14 ledger
  P15 balance projection

Trading
  P16 reservation
  P17 order intake
  P18 matching
  P19 settlement
  P20 fee
  P21 market data

Wallet
  P22 deposit listener
  P23 deposit credit
  P24 withdrawal request
  P25 approval / signing / broadcast

Control
  P26 risk
  P27 admin
  P28 audit / compliance
  P29 API hardening
  P30 frontend UX

Launch
  P31 observability
  P32 disaster recovery
  P33 security hardening
  P34 load testing
  P35 business ops
  P36 launch gate
```

## Milestone definition

- Internal alpha：可以跑完整模擬交易，但不碰真實資金。
- Closed beta：可以在受控環境做有限資金流程，提款仍需高度人工 gate。
- Production launch：所有 readiness gates 通過。

# 生產路線圖

## 依能力規劃路線圖

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
- 正式上線：所有 readiness gates 通過。

## P20 後加速執行軌

P20 已完成並批准的是 contract trading sandbox foundation；後續加速的目標是縮短
等待與規格返工，不是刪除 production safety capability 或把多個高風險 runtime 合併成
未審核的大型變更。P21-P36 仍須依序實作、驗證與 review，不能跳 phase。

```text
P20 已批准的 sandbox foundation
  |
  v
Wave A: P21 市場資料與正式交易缺口收斂
  |
  v
Wave B: P22-P25 錢包與真實資金安全鏈
  |
  v
Wave C: P26-P30 風控、管理、稽核、API 與正式交易 UX
  |
  v
Wave D: P31-P36 可觀測性、復原、資安、壓測、營運與上線門檻
```

### 加速規則

- 每個 phase 只施工一張已批准的 task card；同一 task 完成後立即執行針對性測試、完整測試、task status 與 review draft，避免把驗證累積到 phase 尾端。
- 目前 phase 施工期間，可以為下一 phase 準備唯讀 task card、測試 fixture、資料契約與 review checklist；不得實作下一 phase 的 runtime、schema mutation 或對外 API。
- P16-P20 現有 sandbox boundary 的 production closure 必須逐項明確補齊：matching/fill、position/margin lifecycle、fee/funding、ledger/balance/reservation、settlement/reconciliation；不得以 P20 approved 或 P21 market data 完成推定這些能力已存在。
- 涉及 ledger、balance、reservation、matching、settlement、fee、wallet、risk、admin 或 security 的 runtime，仍須獨立 `HUMAN_REVIEW_REQUIRED`；不得因加速軌而合併或省略 review。
- 每個 phase review 未獲人類批准前，不得開始其相依 phase 的 runtime 實作；加速只允許非 runtime 的準備工作。

### 關鍵路徑與不可壓縮門檻

```text
正式合約交易最小關鍵路徑
  P21 市場資料 -> P26 風控 -> P27 管理 -> P28 稽核 -> P29 API -> P30 UX
                    |                                           |
                    +-> P16-P20 production closure gates <-------+
  -> P31 可觀測性 -> P32 復原 -> P33 資安 -> P34 壓測 -> P35 營運 -> P36 上線門檻

真實資金最小關鍵路徑
  P22 chain listener -> P23 deposit credit --+
                                             +-> P28 audit/compliance -> P31-P36 launch gates
  P24 withdrawal request -> P25 approval/signing/broadcast --+
```

上述兩條路徑都通過後，才可能評估正式上線；其中任一項未完成，都只能維持 sandbox、internal alpha 或受控驗證範圍。

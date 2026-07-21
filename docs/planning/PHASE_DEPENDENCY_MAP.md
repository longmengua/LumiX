# 階段依賴圖

## 權威施工順序

```text
P20 已批准 sandbox foundation
  |
  v
P21 -> P22 -> P23 -> P24 -> P25 -> P26 -> P27 -> P28
    -> P29 -> P30 -> P31 -> P32 -> P33 -> P34 -> P35 -> P36
```

上圖是 runtime 的不可跳階順序；任一 phase 未完成 review，不得開始下一 phase runtime。本文件中的能力分支只用於規劃與風險分析，不提供平行實作授權。

## 能力相依圖

```text
P21 市場資料 -----> P26 風控 --------> P27 管理 --------> P28 稽核

P13 identity / asset --+-> P22 地址 / 鏈上觀測 -> P23 入金 confirmation / credit
P14 ledger -----------+                                  |
P15 balance/reconcile -+---------------------------------+-> P28 稽核證據

P13 identity / auth ---+-> P24 提款請求 -> P25 approval / signing / broadcast
P14 ledger ------------+                                  |
P15 balance/reconcile -+----------------------------------+-> P28 稽核證據
P16 reservation -------+

P26 + P27 + P28 -------> P29 API -> P30 UX -> P31 observability
P23 + P25 + P31 -------> P32 DR -> P33 security -> P34 load -> P35 ops -> P36 launch gate
```

## Review implication

- P21 的完整 task cards 已草擬但未批准；P22–P36 的規劃草案也未提供 implementation approval。
- P22 observation 不代表 P23 credit；P24 request 不代表 P25 approval/sign/broadcast；每一資金邊界必須各自人審。
- P16–P20 的 sandbox foundation 仍不等於 production closure。matching/fill、position/margin、fee/funding、ledger/balance/reservation 與 settlement/reconciliation 缺口不能被任何後續 phase 自動視為完成。
- 詳細規劃與風險門檻請讀 `PHASE_21_36_PLANNING_PROGRAM.md`。

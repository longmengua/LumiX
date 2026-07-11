# Phase 15 - Balance Projection Rebuild Gate

## 目的

這份文件只定義 balance projection rebuild runtime gate 的保守施工範圍。
它只負責把 ledger_entries 重建成 balance_projections read model，不代表正式 balance runtime、reservation runtime 或 futures runtime 已完成。

## 重建邊界

```text
ledger_entries  ->  彙總成 account_id + asset_symbol 的投影資料
account_assets  ->  用來確認該 account / asset 對是否屬於可投影 boundary
accounts.account_type = SPOT  ->  目前只 materialize spot balance projection rows
balance_projections  ->  query-side read model
```

## 計算語意

```text
CREDIT 增加 total_amount
DEBIT 減少 total_amount
available_amount = total_amount
locked_amount = 0
projection_version 由 rebuild batch 明確設定
reconciled_at 必須更新
```

## 保守規則

```text
如果彙總後 total_amount 為負，視為資料不一致並拒絕本次 rebuild
rebuild 是 explicit process，不能由 order / matching / settlement 偷寫
balance_projections 仍然不是 source of truth
```

## HUMAN_REVIEW_REQUIRED

```text
任何 balance projection runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 projection 誤寫成資金真相來源的行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 spot projection gate 擴張成完整 trading runtime 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```

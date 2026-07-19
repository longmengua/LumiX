# Phase 20 - Contract Trading Integration Gate

## 狀態

```text
in progress — T01 completed
```

## 目標

建立較完整的 contract trading sandbox integration gate，覆蓋 failure cases、reconciliation checks 與 admin / audit review，但仍不宣稱 production-ready。

## Sandbox 內容

```text
full sandbox contract trading flow
failure cases
reconciliation checks
admin / audit review
no production claim gate
```

## 不在 phase

```text
production-ready claim
formal trading launch
real withdrawal
public contract trading
```

## 高層 task list

```text
T01 full sandbox contract trading flow - completed (pure eligibility integration only)
T02 failure case coverage
T03 reconciliation checks
T04 admin / audit review
T05 no-production-claim gate
```

## Sandbox 限制

```text
P20 是較完整 contract trading sandbox gate，但仍然不是正式交易。
不宣稱 production-ready、不開放正式交易、不接正式提款。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 contract trading / reconciliation / admin / audit runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把這個 phase 誤寫成正式交易上線證明的行為都屬於 HUMAN_REVIEW_REQUIRED。
```

## T01 implementation notes

- Scope: 整合 P18 contract inspection 與 P19 liquidation simulation 結果；inspection 拒絕或 liquidation simulated 時拒絕 flow，其餘只回傳 sandbox-flow eligibility。
- Deliberate boundary: 不重新撮合、不建立 trade/fill、不更新 position、balance、ledger 或 settlement；eligible 不代表可執行交易。

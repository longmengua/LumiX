<!-- 檔案用途：財務匯出、ledger archive restore smoke、不平衡日報的營運 runbook。英文版本位於 ../en/finance-operator-runbook.md。 -->
# Finance Operator Runbook

UTC 財務日結或 durable-ledger 日報不平衡時使用這份 runbook。

## 每日關帳流程

1. 產生日報：
   `GET /api/recovery/finance/daily-report?date=YYYY-MM-DD`
2. 產生所有分類匯出報表：
   `GET /api/recovery/finance/category-export-batch?date=YYYY-MM-DD`
3. 產生 archive eligibility 與 manifest：
   `GET /api/recovery/finance/ledger-archive-eligibility?date=YYYY-MM-DD`
   `GET /api/recovery/finance/ledger-archive-manifest?date=YYYY-MM-DD`
4. hot-path delete 前先跑 restore smoke 與 replay validation：
   `GET /api/recovery/finance/ledger-archive-restore-smoke?date=YYYY-MM-DD`
   `GET /api/recovery/finance/ledger-archive-replay-validation?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`
5. 跑 immutable delete guard，只有 `approved=true` 才能繼續：
   `GET /api/recovery/finance/ledger-archive-delete-guard?date=YYYY-MM-DD`

## 如果日報不平衡

1. 停止受影響日期的 archive/delete。
2. 保存 `entryCount`、`totalDebit`、`totalCredit` 與不平衡 lines。
3. 跑 ledger tamper evidence：
   `GET /api/recovery/reconcile/ledger/tamper-evidence`
4. 如果已知受影響 uid/asset，跑 replay comparison：
   `GET /api/recovery/reconcile/ledger/{uid}/compare?asset=USDT`
5. manual compensation 前先開啟或 claim reconciliation issue：
   `GET /api/recovery/reconcile/issues/open`
6. 修正後重跑 daily report、category export batch、archive restore smoke、replay validation 與 delete guard。

## Scheduler

`FinanceExportScheduler` 預設 `finance.export.enabled=false`。確認日報平衡、archive restore smoke、告警路由與 operator owner 後再啟用。

<!-- 檔案用途：繁體中文 core-v1 smoke-test runbook。 -->
# Core V1 Smoke Runbook

這份 runbook 用於 checkout、migration 或部署後驗證有邊界的 core-v1 baseline。


## 本機依賴檢查

```bash
docker compose up -d
docker compose ps
./mvnw test
```

## 設定檢查

- Managed environment 確認 `spring.flyway.enabled=true`。
- 會改動狀態的 scheduler 預設保持關閉，除非本次執行明確需要：
  - `BONUS_CREDIT_EXPIRY_ENABLED=false`
  - `MARKET_MAKER_HEDGE_EXECUTION_ENABLED=false`
  - `RISK_SNAPSHOTS_ENABLED=false`
- 啟用流量前先確認緊急開關語意：
  - `RISK_CONTROLS_ORDER_ENTRY_HALT`
  - `RISK_CONTROLS_REDUCE_ONLY_MODE`
  - `RISK_CONTROLS_WITHDRAWAL_HALT`
  - `RISK_CONTROLS_LIQUIDATION_HALT`
  - `RISK_CONTROLS_MARKET_MAKER_HEDGE_EXECUTION_HALT`

## API Smoke 範圍

- 下單與撮合：place、amend、cancel-replace、cancel、open orders。
- Recovery：matching replay validation 與 reconciliation report 查詢。
- 風控：mark price update、account risk、liquidation/ADL controls。
- 帳本：account ledger、trial balance、replay comparison、reconciliation issue workflow。
- 做市商：profile save/query、hedge execution dry path、hedge fill callback、hedge reconciliation。

## 結束條件

- 測試通過。
- App 可對本機依賴啟動。
- Protected API paths 依預期需要 admin credentials。
- 沒有 scheduler 非預期改動狀態。
- 已知 production gaps 仍明確記錄在 `current-state.md`。

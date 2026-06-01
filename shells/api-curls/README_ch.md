<!-- 檔案用途：Markdown 文件，記錄 java21-match-hub 的設計、操作或背景說明。 -->
# API curl scripts

每個 shell 只對應一個 API path。沒有環境變數與聚合流程；要改 host、wallet、session、market slug 或 body，直接打開該 `.sh` 手動修改。

從專案根目錄執行：

```bash
./shells/api-curls/prediction/markets-discover-post.sh
./shells/api-curls/prediction/markets-sync-progress-get.sh
./shells/api-curls/exchange/depth-get.sh
```

## Prediction

Markets：

```bash
./shells/api-curls/prediction/markets-get.sh
./shells/api-curls/prediction/markets-discover-post.sh
./shells/api-curls/prediction/markets-sync-post.sh
./shells/api-curls/prediction/markets-sync-reset-post.sh
./shells/api-curls/prediction/markets-sync-progress-get.sh
./shells/api-curls/prediction/markets-retry-post.sh
./shells/api-curls/prediction/markets-price-refresh-post.sh
```

CLOB credentials：

```bash
./shells/api-curls/prediction/clob-api-key-create-post.sh
./shells/api-curls/prediction/clob-api-key-derive-get.sh
```

Session / order：

```bash
./shells/api-curls/prediction/session-init-post.sh
./shells/api-curls/prediction/session-confirm-post.sh
./shells/api-curls/prediction/session-list-get.sh
./shells/api-curls/prediction/session-revoke-post.sh
./shells/api-curls/prediction/session-revoke-all-post.sh
./shells/api-curls/prediction/orders-post.sh
./shells/api-curls/prediction/orders-local-get.sh
./shells/api-curls/prediction/orders-local-one-get.sh
./shells/api-curls/prediction/orders-local-sync-post.sh
./shells/api-curls/prediction/orders-local-cancel-post.sh
./shells/api-curls/prediction/orders-reconcile-post.sh
./shells/api-curls/prediction/orders-trades-get.sh
```

Approval：

```bash
./shells/api-curls/prediction/approve-collateral-allowance-get.sh
./shells/api-curls/prediction/approve-conditional-tokens-status-get.sh
```

User WebSocket：

```bash
./shells/api-curls/prediction/ws-user-status-get.sh
./shells/api-curls/prediction/ws-user-start-post.sh
./shells/api-curls/prediction/ws-user-stop-post.sh
```

## Exchange

```bash
./shells/api-curls/exchange/order-place-limit-buy-post.sh
./shells/api-curls/exchange/order-place-market-sell-post.sh
./shells/api-curls/exchange/order-open-get.sh
./shells/api-curls/exchange/order-all-get.sh
./shells/api-curls/exchange/order-lifecycle-get.sh
./shells/api-curls/exchange/order-projection-get.sh
./shells/api-curls/exchange/order-projection-rebuild-post.sh
./shells/api-curls/exchange/order-projections-get.sh
./shells/api-curls/exchange/depth-get.sh
./shells/api-curls/exchange/market-data-depth-deltas-get.sh
./shells/api-curls/exchange/margin-deposit-post.sh
./shells/api-curls/exchange/margin-withdraw-post.sh
./shells/api-curls/exchange/margin-transfer-post.sh
./shells/api-curls/exchange/margin-transfers-get.sh
./shells/api-curls/exchange/margin-bonus-credit-report-get.sh
./shells/api-curls/exchange/margin-bonus-credit-campaign-report-get.sh
./shells/api-curls/exchange/margin-bonus-credit-campaign-export-get.sh
./shells/api-curls/exchange/margin-bonus-credit-clawback-post.sh
./shells/api-curls/exchange/margin-turnover-summary-get.sh
./shells/api-curls/exchange/margin-turnover-records-get.sh
./shells/api-curls/exchange/margin-turnover-export-get.sh
./shells/api-curls/exchange/margin-turnover-reconciliation-get.sh
./shells/api-curls/exchange/margin-turnover-reconciliation-recent-get.sh
./shells/api-curls/exchange/margin-risk-get.sh
./shells/api-curls/exchange/margin-risk-snapshot-post.sh
./shells/api-curls/exchange/margin-risk-snapshots-post.sh
./shells/api-curls/exchange/margin-risk-snapshot-latest-get.sh
./shells/api-curls/exchange/margin-risk-snapshots-get.sh
./shells/api-curls/exchange/margin-ledger-replay-get.sh
./shells/api-curls/exchange/margin-ledger-replay-compare-get.sh
./shells/api-curls/exchange/risk-price-oracle-put.sh
./shells/api-curls/exchange/risk-price-oracle-get.sh
./shells/api-curls/exchange/market-maker-quotes-post.sh
./shells/api-curls/exchange/market-maker-quotes-active-get.sh
./shells/api-curls/exchange/market-maker-profile-quotes-get.sh
./shells/api-curls/exchange/market-maker-quotes-reconciliation-get.sh
./shells/api-curls/exchange/risk-adl-queue-claim-post.sh
./shells/api-curls/exchange/risk-adl-queue-execute-post.sh
./shells/api-curls/exchange/risk-adl-queue-release-post.sh
./shells/api-curls/exchange/risk-adl-executions-get.sh
./shells/api-curls/exchange/risk-adl-insurance-reconciliation-get.sh
./shells/api-curls/exchange/recovery-recover-post.sh
./shells/api-curls/exchange/recovery-reconcile-accounts-get.sh
./shells/api-curls/exchange/recovery-reconcile-report-post.sh
./shells/api-curls/exchange/recovery-reconcile-reports-get.sh
./shells/api-curls/exchange/recovery-account-position-consistency-get.sh
./shells/api-curls/exchange/recovery-ledger-tamper-evidence-get.sh
./shells/api-curls/exchange/recovery-finance-daily-report-get.sh
./shells/api-curls/exchange/recovery-finance-category-report-get.sh
./shells/api-curls/exchange/recovery-finance-category-export-batch-get.sh
./shells/api-curls/exchange/recovery-ledger-archive-eligibility-get.sh
./shells/api-curls/exchange/recovery-ledger-archive-manifest-get.sh
./shells/api-curls/exchange/recovery-ledger-archive-restore-smoke-get.sh
./shells/api-curls/exchange/recovery-ledger-archive-replay-validation-get.sh
./shells/api-curls/exchange/recovery-trial-balance-snapshot-post.sh
./shells/api-curls/exchange/recovery-trial-balance-snapshot-get.sh
./shells/api-curls/exchange/recovery-outbox-dlq-get.sh
./shells/api-curls/exchange/recovery-outbox-domain-state-consistency-get.sh
./shells/api-curls/exchange/recovery-outbox-dead-replay-post.sh
./shells/api-curls/exchange/recovery-outbox-dead-compensate-post.sh
```

## Suggested Polymarket Flow

1. 填好 `application-dev.yml` 的 `polymarket.wallet.private-key`、`polymarket.wallet.funder-address`、`polymarket.wallet.signature-type: 3`。
2. 執行 `./shells/api-curls/prediction/clob-api-key-create-post.sh`。
3. 將回傳的 `apiKey`、`secret`、`passphrase` 填入 `polymarket.clob.*`。
4. 重啟 Spring Boot。
5. 依序執行 `markets-discover-post.sh`、`markets-sync-progress-get.sh`、`markets-sync-post.sh`、`markets-price-refresh-post.sh`。
6. 執行 `ws-user-start-post.sh` 啟動私有 order / trade 更新。
7. 修改 `orders-post.sh` 裡的 `sessionId`、`marketSlug`、`direction`、`usdtAmount` 後再真實下單。

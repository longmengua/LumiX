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
./shells/api-curls/exchange/depth-get.sh
./shells/api-curls/exchange/margin-transfer-post.sh
./shells/api-curls/exchange/recovery-recover-post.sh
```

## Suggested Polymarket Flow

1. 填好 `application-dev.yml` 的 `polymarket.wallet.private-key`、`polymarket.wallet.funder-address`、`polymarket.wallet.signature-type: 3`。
2. 執行 `./shells/api-curls/prediction/clob-api-key-create-post.sh`。
3. 將回傳的 `apiKey`、`secret`、`passphrase` 填入 `polymarket.clob.*`。
4. 重啟 Spring Boot。
5. 依序執行 `markets-discover-post.sh`、`markets-sync-progress-get.sh`、`markets-sync-post.sh`、`markets-price-refresh-post.sh`。
6. 執行 `ws-user-start-post.sh` 啟動私有 order / trade 更新。
7. 修改 `orders-post.sh` 裡的 `sessionId`、`marketSlug`、`direction`、`usdtAmount` 後再真實下單。

<!-- File purpose: English guide for local API curl scripts. Chinese version: README_ch.md. -->
# API curl scripts

Each shell script maps to one API path. There are no environment variables or combined workflows. To change host, wallet, session, market slug, or request body, edit the corresponding `.sh` file directly.

Run scripts from the project root:

```bash
./shells/api-curls/prediction/markets-discover-post.sh
./shells/api-curls/prediction/markets-sync-progress-get.sh
./shells/api-curls/exchange/depth-get.sh
```

## Prediction

Markets:

```bash
./shells/api-curls/prediction/markets-get.sh
./shells/api-curls/prediction/markets-discover-post.sh
./shells/api-curls/prediction/markets-sync-post.sh
./shells/api-curls/prediction/markets-sync-reset-post.sh
./shells/api-curls/prediction/markets-sync-progress-get.sh
./shells/api-curls/prediction/markets-retry-post.sh
./shells/api-curls/prediction/markets-price-refresh-post.sh
```

CLOB credentials:

```bash
./shells/api-curls/prediction/clob-api-key-create-post.sh
./shells/api-curls/prediction/clob-api-key-derive-get.sh
```

Session / order:

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

Approval:

```bash
./shells/api-curls/prediction/approve-collateral-allowance-get.sh
./shells/api-curls/prediction/approve-conditional-tokens-status-get.sh
```

User WebSocket:

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
./shells/api-curls/exchange/margin-bonus-credit-clawback-post.sh
./shells/api-curls/exchange/margin-risk-get.sh
./shells/api-curls/exchange/margin-risk-snapshot-post.sh
./shells/api-curls/exchange/margin-risk-snapshots-post.sh
./shells/api-curls/exchange/margin-risk-snapshot-latest-get.sh
./shells/api-curls/exchange/margin-risk-snapshots-get.sh
./shells/api-curls/exchange/margin-ledger-replay-get.sh
./shells/api-curls/exchange/margin-ledger-replay-compare-get.sh
./shells/api-curls/exchange/risk-price-oracle-put.sh
./shells/api-curls/exchange/risk-price-oracle-get.sh
./shells/api-curls/exchange/risk-adl-queue-claim-post.sh
./shells/api-curls/exchange/risk-adl-queue-execute-post.sh
./shells/api-curls/exchange/risk-adl-queue-release-post.sh
./shells/api-curls/exchange/recovery-recover-post.sh
./shells/api-curls/exchange/recovery-reconcile-accounts-get.sh
./shells/api-curls/exchange/recovery-reconcile-report-post.sh
./shells/api-curls/exchange/recovery-reconcile-reports-get.sh
./shells/api-curls/exchange/recovery-outbox-dlq-get.sh
./shells/api-curls/exchange/recovery-outbox-dead-replay-post.sh
./shells/api-curls/exchange/recovery-outbox-dead-compensate-post.sh
```

## Suggested Polymarket Flow

1. Fill `polymarket.wallet.private-key`, `polymarket.wallet.funder-address`, and `polymarket.wallet.signature-type: 3` in `application-dev.yml`.
2. Run `./shells/api-curls/prediction/clob-api-key-create-post.sh`.
3. Put the returned `apiKey`, `secret`, and `passphrase` into `polymarket.clob.*`.
4. Restart Spring Boot.
5. Run `markets-discover-post.sh`, `markets-sync-progress-get.sh`, `markets-sync-post.sh`, and `markets-price-refresh-post.sh`.
6. Run `ws-user-start-post.sh` to start private order / trade updates.
7. Edit `sessionId`, `marketSlug`, `direction`, and `usdtAmount` in `orders-post.sh`, then place a real order.

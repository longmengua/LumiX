<!-- 檔案用途：繁體中文 core-v1 release freeze checklist。 -->
# Core V1 Release Checklist

這份清單用來把目前 production-core baseline 收斂成有邊界的 `core-v1` release candidate。它不代表系統已可承載真實資金 production。


## 納入範圍

- 可 replay 撮合 baseline、sequencer lease/fencing、cancel-replace replay、recovery 與 validation。
- Liquidation/ADL baseline：ranking、planning、scan result、decision audit、halt 與 manual review controls。
- 體驗金 ledger 隔離、grant consume/expiry/clawback baseline、expiry scheduler、turnover facts 與 focused reconciliation hooks。
- 可審計 ledger/reconciliation baseline：trial balance、replay comparison、persisted issue workflow 與後台 issue API。
- 做市商 profile/risk、exposure、quote checks、hedge strategy/execution baseline、fill audit、decision-vs-fill reconciliation、venue callback ingestion 與安全 adapter decorators。

## 延後範圍

- 客戶端與管理端 web。
- Polymarket production worker split 與完整 CLOB lifecycle。
- Production WebSocket/SSE gateway scaling。
- 真實 venue-specific hedge adapter credentials、signing 與 callback verification。
- 完整報表、合規、壓測、dashboard、tracing 與 alert manager setup。

## 必跑指令

```bash
./shells/ai-context.sh
./mvnw test
git diff --check
git status --short
```

## Release Gate

- [x] `./mvnw test` 通過。
- [x] `git diff --check` 通過。
- [x] Flyway migration 已在未發布 production schema 前 squash 成單一 `V20260625_init.sql.sql`；core-v1 之後恢復 append-only 版本規則。
- [x] 會改動狀態的 scheduler 預設關閉，除非明確要啟用。
- [x] Protected admin APIs 已被 `/api/market-maker/**`、`/api/recovery/**`、`/api/risk/**` 與相關 security classifier paths 覆蓋。
- [x] current-state 與 TODO 文件指向 freeze task，而不是繼續擴功能。
- [x] 已知 production blockers 有寫清楚，沒有被默認成已完成。

## 驗證紀錄

- `./mvnw test`：2026-06-25 rerun，95 tests passed。
- `git diff --check`：passed。
- Docker clean volume smoke：Flyway 成功驗證並套用 1 個 migration，Hibernate validate 通過。
- API smoke：`/api/ops/metrics`、`/api/market-maker/profiles/enabled`、`/api/depth/BTC-USDT` 均回 200。

## 仍存在的 Production 風險

- Matching worker routing 仍需完整接入 production lease guard。
- MySQL、Redis、Kafka transaction boundaries 尚未完整定義。
- 真實 market data durability、gateway scaling、observability backend、alerting 與 load testing 尚未完成。
- 真實外部 venue/bank/chain integration 需要 signed callbacks、idempotency、replay protection 與 operator runbooks。

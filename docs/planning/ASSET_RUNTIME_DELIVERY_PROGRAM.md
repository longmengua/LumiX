# 真實資產功能交付計畫與進度

## 目的

將目前前台資產／錢包畫面由 mock adapter 遷移至可追溯的真實資料來源，依既有 Phase 14、22、23、24、25、27 的安全相依順序交付。此計畫不授權任何帳本 mutation、入金 credit、提款簽章、鏈上廣播或 production launch。

## 交付原則

- 正式資產頁不得讀取 `mockAssetService`、`mockWalletService` 或任何前端寫死餘額、地址、交易紀錄。
- 真實餘額只能由 ledger 的可重建 read model 讀取；`balance_projections` 不是資金真相來源。
- 任何 API response 必須清楚區分 loading、empty、資料錯誤、resource not found、授權拒絕與資料新鮮度不足。
- 不可因 API 尚未完成而以 mock／假成功取代；必須顯示受控 unavailable/error state。
- 每完成一張 task，必須更新本文件的狀態、`AI_PROGRESS.md` 與對應 phase README，並記錄測試證據。

## 任務總表

| Task | 範圍 | 依賴 | 狀態 | 完成條件 |
| --- | --- | --- | --- | --- |
| ASSET-T00 | mock inventory、正式路徑隔離與遷移設計 | 本計畫 | `COMPLETED_FOR_PLANNING` | 列出所有 mock consumer、替換次序與禁止接線規則 |
| ASSET-T01 | 真實唯讀資產帳戶／餘額 projection query contract 與 API | Phase 14/15 read model、P29 auth | `COMPLETED_FOR_READ_ONLY_PROJECTION_API` | 僅讀取 authenticated owner 的 projection；沒有餘額 mutation；資料 freshness/source evidence 可見 |
| ASSET-T02 | 前台資產總覽、現貨／合約／槓桿頁切換至 ASSET-T01 API | ASSET-T01、P30 trusted UX | `COMPLETED_FOR_READ_ONLY_PROJECTION_PRESENTATION` | 移除正式頁的 asset mock adapter；完整 loading/empty/error/not-found state；無假資料 fallback |
| ASSET-T03 | 真實資產歷史唯讀 query 與前台歷史呈現 | ASSET-T01、audit/ledger read boundary | `PLANNED` | owner scoped、cursor pagination、immutable event/reference；不暴露其他使用者資料 |
| ASSET-T04 | 內部劃轉 request 與 reservation/ledger handoff | ledger/reservation runtime、risk policy | `BLOCKED_BY_RUNTIME_DEPENDENCY` | idempotency、雙方帳戶一致性、原子數量、audit 與 fail-closed；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T05 | 入金地址派發與鏈上觀測 runtime | Phase 22 provider-specific task、secret/DB approval | `BLOCKED_BY_PHASE_22_GATE` | 沒有 credit；network/address ownership、provider health、reorg/finality evidence 完整；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T06 | 入金 eligibility、credit 與 reversal handoff | ASSET-T05、Phase 23 ledger runtime | `BLOCKED_BY_PHASE_23_GATE` | immutable ledger append、idempotency、reorg correction、reconciliation；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T07 | 提款 request、地址簿、風控與 hold handoff | Phase 24 runtime、risk/ledger/reservation | `BLOCKED_BY_PHASE_24_GATE` | server time restriction、idempotency、fee version、audit、cancel lifecycle；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T08 | 提款 approval、signer、broadcast 與確認 | ASSET-T07、Phase 25 signer-specific task | `BLOCKED_BY_PHASE_25_GATE` | secret isolation、dual-control、immutable evidence、broadcast reconciliation；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T09 | 管理端資產／錢包唯讀檢視與稽核 evidence | ASSET-T01/03/05/07、Phase 27/28 read boundary | `PLANNED` | admin RBAC、資料來源/freshness、不可手動調帳、不可 bypass 風控 |

## ASSET-T00：mock inventory 與遷移設計

```text
狀態：COMPLETED_FOR_PLANNING
完成日期：2026-09-15
```

目前 mock consumer：

| 路徑 | 現況 | 遷移目標 |
| --- | --- | --- |
| `web/src/features/assets/mockAssetService.ts` | 資產總覽、帳戶分頁、資產歷史 mock | ASSET-T01/02/03 真實唯讀 API |
| `web/src/features/assets/wallet/mockWalletService.ts` | 入金、提現、地址簿與紀錄 mock | ASSET-T05/06/07/08 的受控真實 runtime |
| `web/src/features/assets/useAssetOverviewMock.ts` | 將前端 mock 接入正式資產頁 | ASSET-T02 移除並替換為真實 API hook |
| `web/src/features/assets/wallet/useWalletWorkspaceMock.ts` | 將前端 mock 接入錢包頁 | ASSET-T05 至 T08 完成前不得以假資料替代真實狀態 |

遷移順序固定為：先 ASSET-T01 真實唯讀資料，再 ASSET-T02/03 前台讀取；入金與提款的任何寫入／鏈上能力只能依後續 phase gate 啟動。現有 mock 畫面不得被宣稱為資產功能已完成。

## 進度更新格式

每次 task 完成時，在本節追加一筆：

| 日期 | Task | 狀態 | 實際資料來源／禁止範圍 | 驗證 |
| --- | --- | --- | --- | --- |
| 2026-09-15 | ASSET-T00 | `COMPLETED_FOR_PLANNING` | 僅盤點與遷移設計；未接入資料庫、ledger、provider 或資金 mutation | 文件交叉檢查 mock consumer 與 phase gate |
| 2026-09-15 | ASSET-T01 | `COMPLETED_FOR_READ_ONLY_PROJECTION_API` | `GET /api/v1/assets/balances` 只由 server session principal 決定 owner，直接讀取 `accounts`、`assets`、`balance_projections` 中已 materialize 的資料列；amount 以十進位字串回傳，並回傳 projection version、時間與 reconciliation freshness。沒有 account 建立、projection rebuild、ledger / balance mutation、入金、提款或 mock fallback。資料庫／投影查詢失敗由既有 API error boundary 去敏處理；空集合代表沒有既有 projection，不補造零餘額。 | `BalanceProjectionQueryServiceTest`、`JdbcBalanceProjectionQueryRepositoryTest` 通過；驗證 owner predicate、只讀 SQL、空資料不 fallback 與 NUMERIC 精度邊界 |
| 2026-09-15 | ASSET-T02 | `COMPLETED_FOR_READ_ONLY_PROJECTION_PRESENTATION` | `/assets`、`/assets/spot`、`/assets/futures`、`/assets/margin` 已改讀 T01 API，正式頁不再引用 `mockAssetService`／`useAssetOverviewMock`。UI 只顯示 API 已提供的 available / locked / total、projection source、最新 projection 時間與 reconciliation freshness；沒有可信價格或歷史來源時，不顯示假總權益、估值、PnL、劃轉按鈕或假歷史。空 projection、資料錯誤、loading 均有獨立受控狀態。 | `npm run typecheck`、`npm run build` 通過；Compose web 重建；`8088/assets` 與 `8088/assets/` 均為 `200` 且留在 8088；未登入 API 為 `401` |

## 目前下一張 task

`ASSET-T03`：建立真實資產歷史的 owner-scoped 唯讀 query 與前台呈現。開始前必須先確認 immutable ledger／audit read boundary 能提供的 event/reference、cursor pagination、資料新鮮度與去敏規則；未具備真實資料來源前不得以 mock history 取代。

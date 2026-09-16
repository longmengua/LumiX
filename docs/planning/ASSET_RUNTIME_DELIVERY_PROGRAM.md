# 真實資產功能交付計畫與進度

## 目的

將目前前台資產／錢包畫面由模擬資料介面遷移至可追溯的真實資料來源，依既有階段的安全相依順序交付。人類已授權帳本入帳、資產保留、內部劃轉、受治理空投、入金、提款、簽章與鏈上廣播的施工；每條資產路徑仍必須先滿足具名 provider、secret isolation、權限、immutable audit、idempotency、reconciliation 與失敗即拒絕的驗收條件後才可啟用。

資產寫入 runtime 的固定施工順序與驗收證據見 `ASSET_RUNTIME_PREREQUISITE_SEQUENCE.md`；任何 ASSET-T04 至 T10
必須先滿足該文件的前置項，不能用管理端 UI 或資料庫直寫跳過。

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
| ASSET-T03 | 真實資產歷史唯讀 query 與前台歷史呈現 | ASSET-T01、audit/ledger read boundary | `COMPLETED_FOR_READ_ONLY_LEDGER_HISTORY_PRESENTATION` | owner scoped、cursor pagination、immutable event/reference；不暴露其他使用者資料 |
| 內部劃轉 | 內部劃轉請求與資產保留／帳本入帳交接 | 帳本入帳、資產保留、帳戶所有權檢查、稽核紀錄服務 | `COMPLETED_FOR_GOVERNED_INTERNAL_TRANSFER_RUNTIME` | 冪等性、來源與目的帳戶同一使用者、原子數量、稽核與失敗即拒絕；需要人工審核 |
| ASSET-T05 | 入金地址派發與鏈上觀測 runtime | Phase 22 provider-specific task、secret/DB approval | `BLOCKED_BY_PHASE_22_GATE` | 沒有 credit；network/address ownership、provider health、reorg/finality evidence 完整；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T06 | 入金 eligibility、credit 與 reversal handoff | ASSET-T05、Phase 23 ledger runtime | `BLOCKED_BY_PHASE_23_GATE` | immutable ledger append、idempotency、reorg correction、reconciliation；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T07 | 提款 request、地址簿、風控與 hold handoff | Phase 24 runtime、risk/ledger/reservation | `BLOCKED_BY_PHASE_24_GATE` | server time restriction、idempotency、fee version、audit、cancel lifecycle；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T08 | 提款 approval、signer、broadcast 與確認 | ASSET-T07、Phase 25 signer-specific task | `BLOCKED_BY_PHASE_25_GATE` | secret isolation、dual-control、immutable evidence、broadcast reconciliation；屬 `HUMAN_REVIEW_REQUIRED` |
| ASSET-T09 | 管理端資產／錢包唯讀檢視與稽核 evidence | ASSET-T01/03/05/07、Phase 27/28 read boundary | `COMPLETED_FOR_USER_ASSET_PROJECTION_READ` | admin RBAC、資料來源/freshness、不可手動調帳、不可 bypass 風控 |
| 空投 | 空投資產受治理命令與帳本入帳／餘額投影交接 | 帳本入帳服務、餘額投影服務、後台權限／稽核證據 | `COMPLETED_FOR_GOVERNED_ADMIN_AIRDROP_RUNTIME` | 具權限操作者、資產資格、資產／活動冪等性、不可變帳本／稽核、對帳；需要人工審核 |
| ASSET-T11 | 新註冊使用者帳戶容器 provisioning | P13 identity/account、P29 registration | `COMPLETED_FOR_ACCOUNT_BOUNDARY` | 每位已驗證新使用者原子建立 SPOT／FUTURES／MARGIN 空帳戶；不建立餘額或 ledger entry |
| ASSET-T12 | 使用者帳戶 inventory API 與前台狀態 | ASSET-T11、P29 auth | `COMPLETED_FOR_READ_ONLY_ACCOUNT_INVENTORY` | owner scoped 顯示既有帳戶容器，清楚區分帳戶不存在與尚無資產 projection |

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
| 2026-09-15 | ASSET-T03 | `COMPLETED_FOR_READ_ONLY_LEDGER_HISTORY_PRESENTATION` | `GET /api/v1/assets/history` 僅由 authenticated session principal 決定 owner，從既有 immutable `ledger_journals`／`ledger_entries` 讀取其所屬 account entry；以 `(posted_at, ledger_entry_id)` keyset cursor 和 1–100 筆上限讀取。前台 `/assets` 只呈現既有 immutable entry，無資料時顯示 empty state，沒有 mock history、餘額／帳本 mutation、入金、提款或對手方資料。 | `AssetLedgerHistoryQueryServiceTest`、`JdbcAssetLedgerHistoryQueryRepositoryTest`、`JdbcAssetLedgerHistoryQueryRepositoryIntegrationTest` 通過；驗證 owner isolation、只讀 SQL、keyset continuation 與 NUMERIC(36,18) 精度 |
| 2026-09-15 | ASSET-T09 | `COMPLETED_FOR_USER_ASSET_PROJECTION_READ` | `GET /api/admin/v1/users/{userId}/assets` 僅允許 active super-admin 讀取指定使用者既有 `balance_projections`；另以 `/assets/accounts` 唯讀呈現帳戶容器，使空帳戶與無 projection 明確區分。管理端使用者詳情顯示帳戶 type/status、asset 與 total。沒有 admin adjustment、空投、ledger/projection mutation、mock fallback 或對手方資料。 | `JdbcAdminUserAssetQueryRepositoryTest`、既有資產 history 聚焦測試與 web typecheck 通過；API／admin-web 容器重建 |
| 2026-09-15 | ASSET-T00 follow-up | `COMPLETED_FOR_FORMAL_ROUTE_ISOLATION` | `/assets/transfer`、入金、提款、地址簿與其歷史 route 已改為受控 unavailable state，正式 browser route 不再載入 asset/wallet mock adapter 或顯示可執行資產操作；既有 mock 僅保留原始碼供隔離開發／測試，不能經正式 router 到達。 | 前端 typecheck/build 與 route import 檢查 |
| 2026-09-15 | ASSET-T11 | `COMPLETED_FOR_ACCOUNT_BOUNDARY` | email 雙驗證完成後的同一 primary transaction 現在會建立固定識別的 SPOT／FUTURES／MARGIN `accounts` rows；它們只是零餘額帳戶容器，沒有 `balance_projections`、ledger entry、資金 mutation 或 mock。 | 後端 compile；註冊 transaction rollback 仍由既有 `@Transactional` 邊界保護 |
| 2026-09-15 | ASSET-T12 | `COMPLETED_FOR_READ_ONLY_ACCOUNT_INVENTORY` | `GET /api/v1/assets/accounts` 只由 session principal 決定 owner，讀取真實 `accounts` 容器；`/assets` 顯示其 type/status，不補造餘額或 projection。 | 前後端 compile/typecheck |
| 2026-09-15 | 資產 runtime 前置 1：帳本入帳 | `COMPLETED_FOR_INTERNAL_LEDGER_POSTING` | internal-only `TransactionalLedgerPostingService` 在同一 transaction 內完成 idempotency、帳本雙分錄、outbox 與 immutable audit evidence；驗證帳戶／帳戶資產／資產狀態及 precision，從不直接寫 `balance_projections`。無 HTTP endpoint、無 mock runtime、無 provider/wallet/signing/broadcast 接線。 | `TransactionalLedgerPostingServiceTest` 與真實 PostgreSQL 的 `TransactionalLedgerPostingServiceIntegrationTest` 通過；驗證成功 append evidence 與同 payload idempotent replay |
| 2026-09-15 | 資產 runtime 前置 2：餘額投影 | `COMPLETED_FOR_LEDGER_DERIVED_PROJECTION` | posting transaction 只對受影響的 USER account/asset 從 immutable ledger 重新加總後 upsert `balance_projections`；EXCHANGE account 保留在 ledger source of truth，絕不顯示為使用者資產。`V019` 加入 `account_category` 以保護此邊界；本輪沒有 reservation，所以 available 暫等於 total、locked 為零。 | 真實 PostgreSQL integration test 驗證 projection 與 journal 同 transaction、精度維持、idempotent replay 不重複 evidence |
| 2026-09-16 | 空投 | `COMPLETED_FOR_GOVERNED_ADMIN_AIRDROP_RUNTIME` | `POST /api/admin/v1/assets/airdrops` 僅接受 admin session 並由 server-side active super-admin 再次授權。`V021` 建立不可登入的 EXCHANGE 對應帳戶；每次空投以其 DEBIT、目標使用者帳戶 CREDIT 的 immutable `ADJUSTMENT` journal 入帳，絕不直接寫 projection。活動 ID + 用戶 + 帳戶 + 資產形成固定 idempotency key；相同 payload 安全回放、任何不同 payload fail closed。後台 `/assets` 已改為真實表單，沒有 mock 成功或餘額 fallback。 | `AdminAirdropServiceIntegrationTest`、`TransactionalLedgerPostingServiceTest`、後端 compile、前端 typecheck/build 通過；需要人工審核 |
| 2026-09-16 | 內部劃轉 | `COMPLETED_FOR_GOVERNED_INTERNAL_TRANSFER_RUNTIME` | `POST /api/v1/assets/transfers` 僅由 session principal 決定 owner，來源與目標帳戶均由 server 解析且必須同一使用者、不同類型、ACTIVE。`V022` 新增獨立 transfer idempotency scope；同一 primary transaction 內依序建立來源 HOLD、capture、append source DEBIT／destination CREDIT journal、重建兩端 projection 與追加 audit。餘額不足、帳戶／資產不可用或不同 payload 重送均 fail closed；前台 `/assets/transfer` 不再載入 mock adapter。 | `InternalTransferServiceIntegrationTest`、`TransactionalLedgerPostingServiceIntegrationTest`、前端 typecheck/build 通過；需要人工審核 |

### ASSET-T11 migration 與 rollback

`V018__provision_default_user_accounts.sql` 對既有 `users` 補齊三種空帳戶容器，採 `(user_id, account_type)` unique constraint 的 `ON CONFLICT DO NOTHING`，可安全重跑且不會覆蓋既有帳戶。它沒有資產數量、projection 或 ledger 寫入。若需 rollback，必須在維護窗口確認該帳戶沒有 `balance_projections`、`ledger_entries`、reservation 或其他引用後，才可依精確 `account_id` 手動刪除；不得以廣泛 SQL 刪除帳戶。

## 目前下一張 task

施工授權紀錄：2026-09-15 人類解除內部劃轉與受治理空投所需的帳本入帳／資產保留服務施工禁令。下一步從帳本入帳服務開始，接著完成餘額投影更新／對帳／資產保留；不得用模擬資料、介面假成功或資料庫直寫跳過。

空投 runtime 已完成，屬 `HUMAN_REVIEW_REQUIRED`：它不是前端加餘額，也不是未受控的管理員調帳。帳戶凍結只限制轉出給他人或轉到外部，不阻擋空投轉入。金額上限、頻率、風險評分與雙人覆核仍是後續治理能力，不得宣稱已具備。

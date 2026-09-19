# LumiX UI 實作快照

> 本文件承載會隨程式碼變動的路徑、API 與頁面狀態。穩定的視覺與 UX 規則仍以 [`LUMIX_UI_GOVERNANCE.md`](./LUMIX_UI_GOVERNANCE.md) 為準；這裡不是第二套設計規範。

## 快照資訊

- 最後更新：2026-09-19
- 基準 revision：`2b95ebb`
- 工作區狀態：未提交；本快照以目前 working tree 為準
- 前端：React 19.1、TypeScript 5.8、Vite 6.3、React Router DOM 7.6
- 樣式：`web/src/styles/global.css` 與 `web/src/admin/components/AdminPageHero.css`
- 前端根目錄：`web/`

## 共同實作

| 元件                       | 路徑                                                           | 責任                                                                                             |
| -------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `AdminPageHero`            | `web/src/admin/components/AdminPageHero.tsx`                   | 只負責呈現 icon、標題、描述、chips、插圖與價值主張 slot；不含資料請求或 mutation                 |
| `AssetAdjustmentArtwork`   | `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx` | 透明 WebP `picture`，依 viewport 載入 desktop／medium／small 資產                                |
| `AssetAdjustmentPanel`     | `web/src/admin/features/assets/AssetAdjustmentPanel.tsx`       | 資產調整頁層；只掛載唯一的通用資產沖銷／調整表單                                                 |
| `AssetAdjustmentForm`      | `web/src/admin/features/assets/AssetAdjustmentForm.tsx`        | MANUAL_CORRECTION、COMPENSATION、BUSINESS_REVERSAL；套用 Institutional Blue Hero 與 form surface |
| `AdminFormSelect`          | `web/src/admin/components/AdminFormSelect.tsx`                 | 共用受控下拉；提供深色 menu、選取狀態、鍵盤開啟與 Escape 關閉／焦點返回                          |
| `AssetReconciliationPanel` | `web/src/admin/features/assets/AssetAdjustmentAuditPanel.tsx`  | 唯讀對帳核驗；保留 `AssetAdjustmentAuditPanel` 舊名稱別名以相容既有 import                       |
| `adminUserAssetSearch`     | `web/src/admin/features/assets/adminUserAssetSearch.ts`        | 以 adapter 統一名稱前綴查詢、精確 UUID detail 查詢與資產 projection                              |

## 資產管理頁面

### `/admin/assets/users`

- 入口：`web/src/admin/features/assets/AdminAssetsPage.tsx`
- 面板：`AssetUserSearchPanel`
- 查人：共用 `web/src/admin/api/adminUsersApi.ts` 的 `findAdminUsers`（display name prefix）；輸入 UUID 時使用既有 `getAdminUser` 精確查詢。
- Email 搜尋尚無後端能力，正式 UI 會明確標示不可用。
- 資產明細：`getAdminUserAssets`；逐筆顯示 `accountType`、`assetSymbol`、`available`、`locked`、`total`，不跨 Spot／Futures 自行加總。

### `/admin/assets/analysis`

- 面板：`AssetAnalysisPanel`
- tab 由 URL `?tab=overview|ledger|reconciliation|revenue-share|organization` 保存。
- 只有 `reconciliation` 讀取 `GET /api/admin/v1/assets/audit/adjustments`；其餘維度顯示正式 empty state，不建立假數值。
- 前端以 `fetchAssetAdjustmentReconciliation`／`AdminAssetReconciliationItem` 命名資料責任；歷史 API path 與相容 service/type alias 保留不變。
- 舊 `/admin/assets/audit` 由 `AdminAssetsPage` redirect 至 `analysis?tab=reconciliation`。

### `/admin/assets/adjustments`

- 頁面：`AdminAssetsPage` → `AssetAdjustmentPanel` → `AssetAdjustmentForm`。舊 `GovernedAirdropForm` 已移除，通用調整表單保留完整 command 能力並沿用 Institutional Blue 設計；調整類型、方向與資產使用共用 `AdminFormSelect`。
- 資產選項：`GET /api/admin/v1/assets/adjustments/configuration`，只列後端 ACTIVE 現貨資產。
- 通用調整：`POST /api/admin/v1/assets/adjustments`，以 `Idempotency-Key` 保護。支援 `MANUAL_CORRECTION`、`COMPENSATION`、`BUSINESS_REVERSAL`，所有 amount 為正 decimal string。
- 來源查詢：`GET /api/admin/v1/assets/adjustments/reversal-sources/{ledgerEntryId}` 只回傳窄範圍 authoritative source、已沖回與剩餘額；不建立全域 ledger browser。
- 舊 `/api/admin/v1/assets/airdrops` 寫入與設定端點已移除；歷史資料仍保留 immutable evidence，不會 backfill 或猜測來源關係。
- Generic Adjustment 使用 `system:asset-adjustment:spot` 的 `ASSET_ADJUSTMENT_COUNTERPARTY` purpose；目前 UI 與 command capability 只支援 SPOT。
- 所有入帳、冪等、權限、ledger 與 audit 行為均在 server；前端只保留欄位驗證、確認摘要與請求狀態。

## 使用者頁面

### `/admin/users/list`

- 入口：`AdminUsersWorkspacePage` → `AdminUsersPage`。
- 使用者清單與資產頁共用 `findAdminUsers`；目前後端搜尋條件仍只有 `displayNamePrefix` 及日期範圍。
- 管理抽屜為 `UserManagementDrawer`，登入／提幣限制分別使用既有受保護 PUT API；未存在的轉帳、現貨、合約限制只能呈現停用結構。
- 使用者 Hero 與資產頁共用 `AdminPageHero`，使用者插圖仍是頁面內 CSS orb decoration。

## Backend data gaps

- 沒有 Email 搜尋 endpoint。
- 沒有非 UUID 的 User ID 搜尋 endpoint；目前精確查詢依既有 UUID user id detail route。
- 沒有可供 overview、ledger、revenue-share、organization 使用的真實分析資料 API。
- 沒有可支援手續費收入、營收、損益或跨帳戶 authoritative total 的資料來源。

## Current visual gaps

- 資產調整表單的 `FormSelect` 仍是 feature-local listbox；尚未抽成跨頁共用的可及選單元件。
- 部分舊頁面仍有局部色值與狀態色，尚未全面遷移至語意 token；此次未擴大 CSS refactor。

此快照只描述目前可驗證的實作；新增或移除 endpoint、component、token、tab 或 responsive 行為時，請同步更新本文件，並在視覺政策或 UX 原則變動時更新治理文件。

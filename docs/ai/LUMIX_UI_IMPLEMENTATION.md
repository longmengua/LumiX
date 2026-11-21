# LumiX UI 實作快照

> 本文件承載會隨程式碼變動的路徑、API 與頁面狀態。穩定的視覺與 UX 規則仍以 [`LUMIX_UI_GOVERNANCE.md`](./LUMIX_UI_GOVERNANCE.md) 為準；這裡不是第二套設計規範。

## 快照資訊

- 最後更新：2026-11-19
- 基準 revision：`2b95ebb`
- 工作區狀態：未提交；本快照以目前 working tree 為準
- 前端：React 19.1、TypeScript 5.8、Vite 6.3、React Router DOM 7.6
- 樣式：`web/src/styles/global.css` 與 `web/src/admin/components/AdminPageHero.css`
- 前端根目錄：`web/`

## 共同實作

- 現貨與合約使用唯一 `TradingMarketChart.tsx` 圖表模板，搭配 `TradingChartControls.tsx` 與 `ChartPreferences.ts`。控制列為日線／週線／月線／分線，下拉提供 1min／5min／15min／60min；均線可改週期與開關、取消草稿、恢復預設。預設 MA5／20／60／120，localStorage 只保存視覺偏好，同源頁面與分頁同步。
- `FuturesMarketChart.tsx` 已退出交易路由；現貨經 `marketCandles.ts` 驗證所選週期、UTC 連續性與精確價量後傳入同一模板。舊 `SpotCandleChart.tsx` 已移除，不保留第二份繪圖。
- 合約工作區交易對統一呈現 `BTCUSDT`／`ETHUSDT` 格式，不加 `-PERP`；解析時須先辨識報價幣與分隔符，避免重複拼接 USDT。
- `/spot/:symbol` 使用可匿名訂閱的唯讀 Binance 參考行情推播；目前僅 BTC/USDT、ETH/USDT，需同源 HTTPS／WSS，斷線或不可用時不顯示舊價位。該頁不代表 LumiX 的正式委託簿、成交或下單功能已開放；合約頁共用真實 API 面板，風險 runtime 未完成所以無開倉能力。
- 現貨提供真實 UTC 日／週／月與 1／5／15／60 分 K 線；後端固定十四條 K 線來源連線，單一訂閱只推播所選週期，最多 320 根歷史，畫面顯示最近 160 根。月線依曆月、週線依 UTC 星期一；來源歷史不足時不補造。切換期間不顯示上一週期資料，過期或斷線清除舊圖。
- 依使用者指定的 `/futures/BTC-USDT` 排版，現貨與合約共用 `TradingWorkspaceLayout.tsx`，沿用 `market-header`、`market-chart`、`trading-workspace-grid`、`trading-book`、`trading-form`、`balance-panel`：摘要／全寬圖表／左訂單簿右下單面板／底部帳戶區。767px 以下沿用共用垂直排列，現貨 1023px 以下買賣盤單欄，精確數值可換行。訂單簿使用約十列高的可捲動視窗保留完整二十檔，賣盤預設追蹤最佳賣價，手動捲動時保留位置。
- 共用圖表以固定精度整數計算均線與畫面縮放；成交量文字使用獨立可讀字級，不隨 SVG 壓縮。`SpotReferenceOrderBook` 保留精確累計及可見深度比例。現貨下單面板已接真實 API，需政策與設定啟用；帳戶來自 authenticated primary 查詢。合約共用真實查詢及版型，風險 runtime 未完成前不開倉。
- 本機執行 `server/scripts/start-market-preview.sh` 後以 `https://localhost:8088/spot/BTC-USDT` 或 `/spot/ETH-USDT` 驗收；Vite 使用明確憑證啟用 HTTPS，僅轉送固定行情 WebSocket 並覆寫 forwarded headers。一般開發仍預設 HTTP；行情不提供明文降級。
- 2026-11-19 已完成真實來源及瀏覽器本機視覺驗證：買賣各 20 檔、120 根一分鐘 K 線，連續 70 秒數值更新，390px 畫面、離線清除與恢復訂閱通過。正式 Nginx 部署與安全審核尚未完成；詳細證據見 `server/docs/binance-market-data-boundary.md`。

| 元件                       | 路徑                                                           | 責任                                                                                             |
| -------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `AdminPageHero`            | `web/src/admin/components/AdminPageHero.tsx`                   | 只負責呈現 icon、標題、描述、chips、插圖與價值主張 slot；不含資料請求或 mutation                 |
| `AssetAdjustmentArtwork`   | `web/src/admin/features/assets/AssetAdjustmentHeroArtwork.tsx` | 透明 WebP `picture`，依 viewport 載入 desktop／medium／small 資產                                |
| `AssetAdjustmentPanel`     | `web/src/admin/features/assets/AssetAdjustmentPanel.tsx`       | 資產沖銷頁層；只掛載唯一的通用資產沖銷表單                                                       |
| `AssetAdjustmentForm`      | `web/src/admin/features/assets/AssetAdjustmentForm.tsx`        | MANUAL_CORRECTION、COMPENSATION、BUSINESS_REVERSAL；套用 Institutional Blue Hero 與 form surface |
| `AdminFormSelect`          | `web/src/admin/components/AdminFormSelect.tsx`                 | 共用受控下拉；提供深色 menu、選取狀態、鍵盤開啟與 Escape 關閉／焦點返回                          |
| `admin-text-input`         | `web/src/styles/global.css`                                    | 管理端文字輸入共用 class；以使用者列表搜尋欄為色彩與 focus UX 基準                                  |
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
- 通用調整：`POST /api/admin/v1/assets/adjustments`，以 `Idempotency-Key` 保護。支援 `MANUAL_CORRECTION`、`COMPENSATION`、`BUSINESS_REVERSAL`，所有 amount 為正 decimal string；表單只接受當次命令需要的欄位，選填備註不作為提交前置條件。
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

- 資產沖銷表單的 `FormSelect` 仍是 feature-local listbox；尚未抽成跨頁共用的可及選單元件。
- 部分舊頁面仍有局部色值與狀態色，尚未全面遷移至語意 token；此次未擴大 CSS refactor。

此快照只描述目前可驗證的實作；新增或移除 endpoint、component、token、tab 或 responsive 行為時，請同步更新本文件，並在視覺政策或 UX 原則變動時更新治理文件。

## 2026-11-21 真實交易接線施工狀態

2026-11-22：`/futures/:symbol` 已改用 `SpotReferenceMarket` 的真實交易面板，不載入 mock 合約工作區。合約表單明確送出 LONG／SHORT、開倉或只減倉、槓桿與委託參數；帳戶區顯示逐倉持倉、真實餘額、委託、成交及 LumiX 自有簿。標記／指數價來自獨立後端永續參考 API，失效時停止新增風險；圖表與左側外部委託簿仍明確標示 Binance 現貨參考，不能當作永續標記價或 LumiX 可成交簿。商品及全域設定預設停用，非 USDT 保證金尚不能啟用。前端已通過 typecheck 與 build；真實雙帳號瀏覽器驗收仍待執行。

- 現貨與合約共用 `SpotReferenceMarket` 與 `TradingLivePanels`；交易路由不再載入 mock wallet、mock 委託或本地假送單。共用 API 面板顯示 session owner 的餘額、委託、成交與獨立 LumiX 彙總簿。
- Binance 現貨圖表／外部深度保留來源標示；合約頁明示這不是永續標記價格。沒有把外部參考深度當作 LumiX 可成交流動性。
- 下單需當前真實快照與後端啟用政策；錯誤、過期、未登入時清除舊資產，不預填零。送單／撤單結果不確定時沿用同一冪等鍵重試，不顯示樂觀成功。
- 現貨 API、政策治理與 UI 正在整合驗證；永續 margin／PnL／funding／liquidation runtime 尚未完成，不能開倉。此條目不是完整交易交付或端到端驗收完成證據。

- 真實帳戶表格以 `trading-account-table` 提供可鍵盤操作的橫向捲動，完整保留委託識別與金融數值；命令請求十秒超時後維持原冪等識別，不推定交易未執行。

- 本次前端 typecheck／build、同鍵重試與 1440／768／390px 瀏覽器邊界測試已通過；尚未完成真實雙帳戶端到端成交驗收。

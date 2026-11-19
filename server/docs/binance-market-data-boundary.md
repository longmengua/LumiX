# Binance 現貨行情接入邊界

狀態：BTC/USDT、ETH/USDT 的唯讀深度與一分鐘 K 線接收、可匿名讀取的 WebSocket 推播與現貨參考行情頁已實作；來源預設關閉，尚未完成正式部署及獨立安全審核。`HUMAN_REVIEW_REQUIRED`。

## 目標與來源

以 Binance 公開現貨資料作為 BTC/USDT、ETH/USDT 的外部參考行情。來源行情與未來 LumiX 自身的成交及委託簿必須分別標示，不能把來源委託簿當成 LumiX 的流動性。使用者表示已簽約；具體授權文件不存放於本文件。

## 來源協定與契約差異

Binance 的深度增量帶有首個更新序號 `U` 與最後更新序號 `u`，一則訊息可涵蓋多個更新。既有 `MarketDataStreamAdmissionPolicy` 假設事件 sequence 逐筆加一，若直接把 `u` 當 sequence，可能將連續資料誤判成缺口。不能用本地自增序號掩蓋來源缺口；必須先設計保留來源區間的驗證與快照／緩衝／重同步，並明確記錄來源序號與事件時間。來源價格及數量字串需以可控精度處理，不得轉成 binary floating point。

`BinanceDepthSequence` 提供純區間判斷：過時區間忽略、涵蓋下一更新的區間可套用、缺口要求重同步。來源接收器對同交易對序列化快照、緩衝與增量；此實作使用獨立唯讀行情簿，沒有接入交易或資產模組。

`BinanceOrderBook` 以精確十進位原子套用快照與增量；缺口、非法資料或斷線後要求新快照，逾時則不對外暴露舊價位。快照必須有連續增量銜接後才可標示健康；來源接收器另外檢查來源時間與有限快照能否覆蓋推播的前 20 檔。

`BinanceKlineFeed` 先連接一分鐘來源串流並緩衝更新，同時讀取最多 120 根 UTC 歷史 K 線；`BinanceKlineSeries` 驗證時間區間、收線、成交筆數及精確十進位價格，避免較舊更新覆寫快照。來源事件與接收時間過期、K 線缺口或內容倒退時，不再推播舊圖並重新同步。資料僅代表 Binance 現貨參考行情，不代表 LumiX 成交。

來源文件：[Binance 現貨 WebSocket 市場資料](https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams)、[Binance 現貨市場資料](https://developers.binance.com/docs/binance-spot-api-docs/rest-api/market-data-endpoints)。

## 傳輸與安全目標

- 上游使用加密 WebSocket（WSS）與 HTTPS，驗證伺服器憑證及主機名稱；不允許降級至明文或關閉驗證。
- 下游推播以 WSS 對外提供；使用者指定由前方 Nginx 終結 TLS，後端不可直接暴露公網，也不得信任未受控來源偽造的代理標頭。仍需限制來源、連線數、訂閱數、訊息大小及傳送佇列，並驗證實際代理部署。
- 斷線、超時、序號缺口、緩衝溢出或資料過期時，標示不可用或降級；重建並驗證連續性之前不得標示健康或供做市使用。
- 日誌不得包含憑證或敏感連線資訊。任何快取、持久化、公開 API 及前端接線都需依各自安全邊界交付；加密不代表可防範所有攻擊。

## 目前推播介面與部署條件

- `infrastructure` profile 下設定 `LUMIX_BINANCE_MARKET_FEED_ENABLED=true` 才啟動接收及 `/api/v1/market-data/stream`；預設停用。`LUMIX_BINANCE_MARKET_FEED_ALLOWED_ORIGIN` 須設定單一明確 HTTPS 網站來源，空值或 HTTP 會拒絕啟動。
- 僅 `GET /api/v1/market-data/stream` 可匿名讀取，帳戶與交易 API 仍須認證；握手必須由受信任 Nginx 帶入經覆寫的安全協定資訊。Nginx 必須處理 WebSocket Upgrade、限制直接存取後端、覆寫而非轉傳使用者提供的 forwarded headers。不可只憑程式中的 `isSecure()` 防止偽造代理標頭。
- 每秒推播兩個來源交易對的前 20 檔與最多 120 根一分鐘 K 線：來源、發布時間、來源事件時間、本機接收時間、健康狀態與十進位字串價位。不健康時不提供舊價位。不顯示假週期選項；其他 K 線週期尚未實作。匿名唯讀推播不綁定帳戶 session，也不再每分鐘強制斷線；數量、緩衝與發送時限仍受限制。
- 限制同時 100 連線、單筆輸入及發送緩衝；不接受客戶端自訂訂閱。此限制是單一程序的保護，不是跨節點 rate limit 或完整阻斷服務防護。
- Binance 實際唯讀 WebSocket／快照整合及本機 HTTPS Vite／瀏覽器鏈路已驗證；正式 Nginx 部署仍未驗證。

## P21-R02 本機視覺驗收交付（2026-11-19）

任務狀態：`READY_FOR_LOCAL_VISUAL_REVIEW`，等待使用者依「能看 K 線、能看訂單簿跳動」驗收；整體 P21-R02 正式行情範圍仍為進行中。`HUMAN_REVIEW_REQUIRED`：精確路徑的匿名行情例外、代理信任及連線資源邊界需要人工審核。

從 `server/` 執行 `./scripts/start-market-preview.sh`。此入口只啟動本機 PostgreSQL／Redis／Kafka 依賴與各自的 API、前台主機程序；不啟用鏈上觀測或提款執行。開啟：

- BTC：`https://localhost:8088/spot/BTC-USDT`
- ETH：`https://localhost:8088/spot/ETH-USDT`

本機預設憑證為自簽，首次開啟需核對 localhost 後由使用者接受；不修改系統信任庫。亦可透過 `LUMIX_DEV_TLS_CERT`／`LUMIX_DEV_TLS_KEY` 提供已受信任的本機憑證。預設私鑰及日誌置於 git 忽略的 `server/target/market-preview/`，私鑰權限為 `600`。這不替代正式憑證或 Nginx 部署。

Vite 明確啟用本機 HTTPS，僅代理固定行情 WebSocket 路徑，覆寫傳入的 forwarded headers；API 與 Vite 均只監聽 loopback。前台 8088 仍拒絕 admin API／HTML，後台 8089 仍只處理管理端；帳戶 API 維持 session 驗證。

使用者另指定 `/futures/BTC-USDT` 為現貨排版參考，本機視覺驗收版型已改用同一 `TradingWorkspaceLayout`：上方摘要、全寬 K 線、中段左側訂單簿與右側下單區、底部帳戶區。摘要只呈現真實一分鐘統計及最佳買賣價，不冒充 24 小時資料或合約指標。下單區停用且無送單 handler；未接入的帳戶資料及成交比例明示尚未提供，沒有模擬餘額、訂單或假成交。

圖表沿用合約的座標系、色彩與間距，以固定精度整數從真實 K 線計算成交量及 MA5／20／60／120；來源只有 120 根，因此 MA120 只有一個有效觀察點，不補造更早資料。訂單簿採三欄價格／數量／累計量，完整買賣各二十檔可在約十列高的視窗內捲動與鍵盤存取；賣盤預設追蹤最佳價，使用者手動捲動時保留位置。767px 以下沿用共用面板垂直排列，現貨 1023px 以下將買賣盤改單欄，精確數值不裁切。

驗證證據：

- 既有行情測試 13 項通過，包含顯式啟用的真實 Binance 深度與 K 線連線；既有提款／安全相關回歸 19 項通過。未新增測試套件。
- 真實瀏覽器連續觀察 70 秒，買賣盤各 20 檔、120 根 K 線；BTC 來源序號由 `103178340141` 前進至 `103178361031`，最新 K 線收盤價由 `63611.29000000` 更新至 `63593.92000000`，均為當次觀察值。
- BTC／ETH 桌面與 390px 手機畫面已檢查；瀏覽器只向 LumiX 同源送出請求，無前端第三方連線或 JavaScript 錯誤。
- 合約版型對齊已檢查 1440／1280／1024／768／390px。1440px 時摘要及 K 線的座標、高度、寬度與合約一致，中段兩欄寬度及起點一致；帳戶與下單的內容高度依實際可用能力呈現。共用骨架抽取前後的合約版型一致；另依使用者指示，合約名稱統一為 `BTCUSDT`／`ETHUSDT`，移除 `-PERP`，修正 `BTC-USDT` 解析後重複拼接 USDT 的問題。
- 瀏覽器離線後清除舊訂單簿與圖表，恢復網路後重新訂閱並顯示真實資料。
- 三個 port 的 15 個路由檢查通過：API HTML 回 404、health 回 200，前台拒絕管理頁、管理端拒絕前台 API，redirect 不跨 port；另確認匿名帳戶 API 回 401。驗證時修正既有 Vite 管理端根入口被誤擋的問題，使 `8089/` 導回同埠的 `/admin/login`。
- `npm run typecheck`、`npm run build` 通過；build 仍有既有 bundle 大小提示。

完整 application 啟動另揭露既有 bean 注入錯誤：三個提款服務同時有 runtime 與可控 Clock 建構子，需明確 `@Autowired`；風控計數服務需明確選擇 `stringRedisTemplate`。本次只修正注入選擇，未變更提款業務規則，實際 application 已成功啟動。

本次沒有 schema 變更；停止本機驗收程序即可關閉此入口。一般設定的行情來源仍預設關閉。

## 尚待完成

正式 Nginx／TLS 部署驗證、其他 K 線週期及成交資訊、來源斷線／慢速用戶的完整故障演練、跨節點限流、監控與人工審核均未完成。使用者已同意新增行情聚焦測試；本機視覺驗收不代表正式行情已可用或 production ready。關閉 `LUMIX_BINANCE_MARKET_FEED_ENABLED` 可停止新來源連線及推播端點，不涉及資料庫 migration。

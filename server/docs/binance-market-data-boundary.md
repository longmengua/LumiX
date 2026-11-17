# Binance 現貨行情接入邊界

狀態：BTC/USDT、ETH/USDT 的唯讀深度與一分鐘 K 線接收、已認證 WebSocket 推播與現貨參考行情頁已實作；來源預設關閉，尚未完成部署及獨立安全審核。`HUMAN_REVIEW_REQUIRED`。

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
- 握手沿用 `/api/v1/**` 的 session 驗證，且必須由受信任 Nginx 帶入經覆寫的安全協定資訊；Nginx 必須處理 WebSocket Upgrade、限制直接存取後端、覆寫而非轉傳使用者提供的 forwarded headers。不可只憑程式中的 `isSecure()` 防止偽造代理標頭。
- 每秒推播兩個來源交易對的前 20 檔與最多 120 根一分鐘 K 線：來源、發布時間、來源事件時間、本機接收時間、健康狀態與十進位字串價位。不健康時不提供舊價位。不顯示假週期選項；其他 K 線週期尚未實作。每連線 60 秒後需重新握手驗證，登出／撤銷並非即時踢除。
- 限制同時 100 連線、單筆輸入及發送緩衝；不接受客戶端自訂訂閱。此限制是單一程序的保護，不是跨節點 rate limit 或完整阻斷服務防護。
- Binance 實際唯讀 WebSocket／快照整合曾在受控執行環境通過；下游握手與推播已有隔離測試，尚未在正式 Nginx、實際認證及瀏覽器整條鏈完成驗證。

## 尚待完成

正式 Nginx／TLS 與認證端到端驗證、其他 K 線週期及成交資訊、斷線／慢速用戶的完整故障演練、跨節點限流、監控與人工審核均未完成。使用者已同意新增行情聚焦測試；在驗證與部署條件成立前不得宣稱正式行情已可用。關閉 `LUMIX_BINANCE_MARKET_FEED_ENABLED` 可停止新來源連線及推播端點，不涉及資料庫 migration。

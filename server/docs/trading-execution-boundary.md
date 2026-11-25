# 交易執行邊界

狀態日期：2026-11-25<br>
狀態：功能已具備，但接單預設關閉；正式開放仍需人工核准（`HUMAN_REVIEW_REQUIRED`）。

## 權威模型

- LumiX 目前以站內客戶委託優先撮合，現貨與合約命令都由後端在單一資料庫交易內完成委託、成交、預留／釋放與帳本紀錄。
- Binance 公開行情只作為市場資料與風控參考；目前沒有 Binance API key、正式外部帳戶授權或正式送單 adapter，因此不得宣稱已在 Binance 對沖。
- 外部整合統一走 provider-neutral 的 `ExternalOrderProxy`／`ExternalOrderRouter`。目前只有後端 Demo router；未來新增 Binance 或其他來源時，呼叫端仍使用相同 intent／outcome 契約。
- 所有回應的 `status`／`reason` 是英文描述，`statusCode`／`reasonCode` 是穩定數字代碼。

## 現貨能力

- 支援 LIMIT／MARKET、GTC／IOC／FOK、站內撮合、部分成交、撤單、資金預留、雙錄帳與命令冪等。
- 自成交會拒絕；價格、數量、tick、step、最小名義額與行情時效都由商品及不可變政策驗證。
- 建立新單使用 `POST /api/v1/trading/spot/orders`；撤單使用 `DELETE /api/v1/trading/spot/orders/{orderId}`，兩者都要求登入、允許的 Origin 與 `Idempotency-Key`。

## USDT 合約能力

- 支援逐倉、LONG／SHORT 雙向持倉、LIMIT／MARKET、GTC／IOC／FOK、reduce-only、資金預留與持倉事件。
- 已有版本化風險政策、手動核定 funding rate、標記價／指數價檢查、強平 evidence、保險基金分錄及風險停機狀態。
- 強平只會使用 LumiX 自有可成交簿；沒有流動性時不會假裝成交。保險基金不足時拒絕虛構兌付。
- 建立新單使用 `POST /api/v1/trading/futures/orders`；撤單使用 `DELETE /api/v1/trading/futures/orders/{orderId}`。
- 非 USDT 保證金只保留擴充介面，尚未完成抵押品折算、USDT 兌付池與正式結算。

## 資料庫交易總開關

- `trading_runtime_settings` 是接單開關的唯一權威來源，包含 `SPOT`、`FUTURES`，baseline 與 seed 預設皆為 `enabled=false`。
- `GET /api/admin/v1/trading/runtime-settings` 讀取目前設定；`PUT /api/admin/v1/trading/runtime-settings/{kind}` 以 `expectedVersion` 更新並在同一交易寫入 audit log。
- 新單、風險政策命令與合約強平掃描每次都重新讀取資料庫，不依賴前端狀態或環境變數。
- 關閉新單時仍允許撤單，避免鎖住既有委託與預留資金。
- 即使開關打開，缺少啟用商品、完整政策、新鮮行情、可用帳戶或已注資保險基金時仍拒絕接單。

## 外部報價與送單 Proxy

- 報價邊界由 `ExternalLiquiditySource`、`ExternalQuotePolicy` 與 `ExternalQuoteCalculator` 組成。政策可設定全站百分比、每單位固定 USDT 以及幣別調整；ask 向上、bid 向下依商品 tick 取整。
- 過期、商品不符、交叉或倒序的外部深度不會產生可用報價。現貨頁可使用公開預覽政策重算顯示價，但目前客戶委託仍只和站內委託成交。
- `external_order_proxy_settings` 保存 Demo 開關，預設為 `true`。管理員以 `GET`／`PUT /api/admin/v1/trading/external-order-proxy/settings` 讀寫，更新具 optimistic version 與 audit log。
- Demo API 位於 `/api/v1/trading/external-proxy/demo`，只允許 loopback 與指定 Origin。支援 RANDOM、NORMAL、DELAYED、PARTIAL、SLIPPAGE_FILLED、SLIPPAGE_REJECTED、VENUE_REJECTED、UNKNOWN_TIMEOUT。
- Demo 結果只存在程序記憶體，不建立正式 LumiX order、trade、reservation 或 ledger；它只用來演練後端體驗，不能當作外部成交證據。

## 行情來源

- 前台現貨訂單簿與 K 線使用 Binance Spot 公開行情；BTC／ETH K 線已接入前台。
- 合約頁目前仍顯示 Binance Spot 參考簿與 K 線，不得標示為 Binance 永續流動性。
- 後端風控另接 Binance USDⓈ-M 公開標記價／指數價，並執行來源名稱、商品與時效檢查。

## 目前本機資料狀態

- 接單總開關：SPOT=false、FUTURES=false。
- seed 只提供基礎資產、BTC-USDT／ETH-USDT 市場、系統帳戶與本機管理員；不自動創造客戶資金、現貨執行政策、合約風險政策或合約商品。
- 因此程式能力存在，但全新本機資料庫預設不能直接建立客戶交易。必須先完成資金、政策與商品配置，再由最高管理員明確開啟對應市場。

## Migration 與驗證

- 完整 schema 已在 2026-11-25 合併至單一 `V001__baseline.sql`；`db/init.sql` 只引用該 baseline，初始配置由可重複執行的 `db/seed.sql` 負責。
- 現貨與 USDT 合約的整合測試覆蓋開／平倉、獲利與虧損、funding、強平、保證金釋放、FOK、自成交、過期行情、冪等與帳本守恆。
- 正式上線前仍需另行完成：真實外部送單 adapter、API key 管理、外部訂單持久化與恢復、部分成交對帳、營運注資、容量／故障演練及人工安全審核。

## 回退原則

- 先在資料庫關閉 SPOT／FUTURES 新單，保留登入查詢、撤單、減倉與必要風險巡檢。
- 已產生的 order、trade、position event、policy、audit 與 ledger evidence 不可刪改；帳務修正只能追加反向或補正分錄。
- Demo Proxy 可獨立關閉，不影響站內撤單；關閉後不得再接受新的 Demo 送單。

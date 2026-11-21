# 現貨與合約客戶下單實作邊界

日期：2026-11-21。`HUMAN_REVIEW_REQUIRED`。

使用者已要求完成現貨與合約客戶下單；本文件將該需求拆成真實交易 runtime 的施工相依，不將表單可點選或 sandbox 通過視為可交易。已完成現貨 runtime 的本機整合實作與共用前端接線；現貨雙帳戶端到端驗收、實際政策啟用與完整永續 runtime 尚未完成。後續依已確認的產品選項及實際技術相依施工，交易與資金變更仍須保留可驗證證據。

## 2026-11-22 產品選擇與施工狀態

使用者核定現貨及合約均由 LumiX 使用者互相撮合；首批現貨為 BTC/USDT、ETH/USDT，合約為 BTCUSDT、ETHUSDT USDT 永續逐倉，且允許同帳戶同商品同時持有 LONG 與 SHORT。費率、槓桿、維持保證金及風險限額採版本化政策，實際啟用數值待核定。後端接入具名標記／指數價格，過期時拒絕新增風險。強平使用 LumiX 委託簿，穿倉由預先注資的保險基金承擔，基金不足或缺流動性時停止增加風險。交付目標為本機完整交易閉環，正式環境啟用另行審核。

保證金初始使用 USDT，未來可由後台政策切換其他抵押幣別。其他幣別使用後端行情價格及後台設定的折算率，必須保存來源、政策版本及有效期限；已實現盈虧與資金費用仍以 USDT 結算。使用者選擇由平台專用 USDT 帳戶預先注資保險基金；非 USDT 抵押品的虧損兌付使用有額度的 USDT 池，按核定匯率扣抵押品。兩者都須有實際資金、額度、不可變帳本及對帳證據，欠缺時不得啟用相應路徑。

目前新增雙向持倉的純成交計算、抵押價值換算與 V005 持倉結構；這些尚未接到客戶命令。V006 追加不可變合約風險政策及獨立兌付池帳戶用途。V001–V006 已在隔離的嵌入式 PostgreSQL 執行 migration 測試通過；既有現貨資金整合 7 項案例也在專用 `lumix_test` PostgreSQL 重跑通過。本機 Docker Compose 的 PostgreSQL、Redis、Kafka 已啟動且健康；真實前後台與雙帳號瀏覽器端到端驗收仍待進行。

`FuturesRiskPolicy` 驗證完整商品、保證金、行情、funding、保險基金及有額度的兌付池欄位，並拒絕維持保證金高於或等於初始保證金的配置。最高管理員合約政策 API 可逐版追加政策與稽核／outbox，同版重送只接受相同內容，核對專用 USDT 系統帳戶；發佈後商品保持 HALTED，不提供開倉入口。Binance USDⓈ-M 公開行情為已選上游，正式使用條款由使用者核對。

接單前風控新增 `FuturesOrderRiskGate`：以具名 mark／index 的來源及時效、委託價和標記價較高者、明確槓桿、持倉名義額上限、手續費與隔離保證金核算；非 USDT 抵押另檢查後端匯率時效、政策折算率、兌付池額度及真實可用餘額。此為純決策，尚未與下單、預留及持倉交易接線，不表示合約可開倉。逐倉政策現亦指定獨立保證金池帳戶，與保險／兌付池分離。

V007 為合約委託保存不可變 LONG／SHORT 方向、reduce-only 與槓桿身分，並保存成交後隔離抵押品。內部 `FuturesHedgePositionStore` 在同交易中核對真實成交、同版風險政策、TRADE journal 的 USDT 分錄，再以版本檢查更新持倉及追加事件。隔離資料庫測試以雙帳戶真實 ledger journal 驗證開倉、部分平倉、全平倉、損益、資金守恆與重送。這是持倉結算的內部步驟；撮合命令、保證金預留、funding、強平及非 USDT 兌付仍未接線，商品保持 HALTED。

已加入預設關閉的 Binance USDⓈ-M `premiumIndex` 後端接收器，固定 BTCUSDT、ETHUSDT，分別保存 mark／index、來源及接收時間。來源失敗即清除快照，讀取時依政策時效拒絕過期資料；兩項解析／失效測試通過。兩個具名公開 HTTP 端點已在本機實際回應，但接收器尚未接到合約風險決策或完成長時間穩定性驗證。官方介面：https://developers.binance.com/en/docs/catalog/core-trading-derivatives-trading-usd-s-m-futures/api/rest-api/market-data 。

## 現有可重用能力與缺口

| 範圍 | 已有程式證據 | 必須補齊的能力 |
| --- | --- | --- |
| 現貨頁 | `web/src/features/trading/SpotReferenceMarket.tsx`、`TradingLivePanels.tsx` | 已接真實 API；仍待核定現貨政策及兩帳戶端到端驗收。Binance 參考簿與 LumiX 自有簿分開呈現 |
| 合約頁 | `TradingWorkspace.tsx` 重用共用真實 API 面板 | 已移除交易路由的 mock 資金／假送單；永續風險閉環未完成，不開倉 |
| 訂單／成交 schema | `server/src/main/resources/db/migration/V001__baseline.sql` 的 orders、trades | 現貨下單／撤單 Controller 與交易寫入 service 已實作；仍無永續持倉／資金費率 runtime schema |
| 市場 schema | markets 的 base／quote 唯一，只有價格／數量 scale | V004 新增獨立商品與不可變現貨執行政策，不能以相同 underlying symbol 混合撮合；永續風險政策待實作 |
| 帳本 | `TransactionalLedgerPostingService`、`LedgerBalanceProjectionUpdater` | 重用 append-only journal 與 projection；驗證跨帳戶鎖定順序、重放及與 reservation 的原子性 |
| 資產保留 | `ReservationHoldService`、`ReservationCaptureService`、`ReservationReleaseService` | 新增 OrderReservationChangeService 按成交額 partial capture/release，保留未成交及釋放價格改善額；原有其他業務 reservation 入口不變 |
| 現貨核心 | `trading/core/spot` 的 orderintake／orderbook／matching／settlement | 現有 sandbox 不可直接暴露作客戶資金 runtime，需要持久化順序、撮合、結算與復原 |
| 合約核心 | `trading/core/futures` 的 order／margin／position 等 pure gate | 缺持久化訂單、持倉、開平倉、PnL 入帳、資金費率、風險監控與強平／穿倉處理 |

既有產品規格：`docs/product/trading-product-scope.md` 指定現貨限價、風控就緒後市價，以及 USDT 永續、逐倉；`fee-and-revenue-policy.md` 指定 quote 計費方向，但沒有可直接啟用的費率、取整與最低費用政策。sandbox 的槓桿型別或 UI 選項不是正式政策。

## 不依賴具體費率的共同不變量

1. owner 只取自登入 principal；browser 不可指定其他 user／account 身分，查詢、撤單及平倉均再次檢查。
2. 價格、數量、費率、保證金與盈虧使用受限長度的十進位字串，後端 BigDecimal；禁止 binary floating point 參與資金計算。過大 exponent、非正數、超 scale／precision／步進均拒絕。
3. 下單及撤單具 owner scope 的 durable idempotency；同鍵不同 payload 拒絕，相同數值不同尾零須有一致 canonical fingerprint，重試不可重複 hold 或成交。
4. 接單前檢查 user／account／asset／instrument／policy 狀態、交易限制、可用額與請求 admission；狀態與餘額在交易內重新檢查。
5. 新單必須先保留資金，才能進入可撮合狀態；ACCEPTED／OPEN 不代表 FILLED。無對手單時掛單或依 IOC／FOK 規格處理，禁止依 Binance 價格假造成交。
6. 價格優先、明確持久化序列的時間優先；maker 價成交；同一 owner 自成交政策明確，不能只比較不同 account ID。市場暫停阻擋新單／撮合，但不任意阻擋 owner 安全撤單。
7. 撮合結果、partial capture、找零釋放、ledger、訂單數量狀態、trade、audit／outbox 同交易提交或具可驗證重放；不能先送成功、事後再扣款。
8. 每資產 journal 守恆；projection 保持 total = available + locked，available／remaining 不可負數。費率與風險 policy 版本須保留在 execution evidence，不能重放時套用新政策。
9. 所有失敗均回滾或保留可對帳的 pending evidence；restart 不可遺失 OPEN 訂單或有效 reservation。跨市場並行也必須避免同帳戶超額花費。
10. 有限未結委託數、請求大小、單筆最大量、交易時間與併發預算；cookie 認證命令具明確的跨站請求防護及同源邊界。

## 建議施工順序與驗收

### D1：共用 admission、商品與資金保留

- 確認交易對手模型及政策來源；建立獨立現貨／永續 instrument identity，保留既有 schema 所有未確認用途的物件，不新增舊 schema adapter 或 dual-write。
- 設計十進位輸入、owner scope、版本化商品／fee／risk 規格、durable command idempotency、有限命令 admission 與錯誤契約。
- 擴充 partial reservation consume／release 與原子稽核；明確驗證與既有提款、劃轉、資產修正共用資產時的鎖順序。單一市場序列化不能取代跨市場 balance lock。
- 查詢來自真實 DB。缺商品／政策／授權／可用資產即拒絕，沒有 runtime fixture 或預設虛構金額。

驗收：同一餘額並行 hold 不超支、partial capture 不消耗剩餘委託資金、重送不重複、owner 隔離、失敗全回滾。

### D2：現貨交易完整閉環

```text
登入客戶 -> 下單驗證／冪等 -> hold -> 持久化委託與順序
  -> LumiX 真實買賣委託撮合 -> 部分／全部成交
  -> 買方 quote 與賣方 base 扣用
  -> base／quote／fee 守恆入帳 -> 更新餘額與委託
  -> 客戶查詢委託／成交／資產
撤單 -> owner／狀態／競態檢查 -> 釋放僅剩餘 hold -> durable CANCELLED evidence
```

先落地限價與撤單；市價需價格保護、可計算的預留上界、slippage、深度不足及 IOC／FOK 行為明確後接入，不將可掛單描述成所有 order types 已完成。公開簿若展示 LumiX 委託，必須與外部參考深度分別標示。

驗收：兩個實際帳號以已授權的本機資產入帳建立雙邊資金，完成掛單、部分成交、價格改善、撤單、重送、斷線查回與重新啟動；ledger／reservation／orders／trades 逐項對帳。整合測試 fixture 僅存在 test source set。

### D3：USDT 永續逐倉風險與結算

先確認 client-to-client 或平台對手盤模型。兩者的資本來源、PnL 債務及清算責任不同，不可共用未審核假設。

- 持久化合約規格、單／雙向持倉模式、開平倉／reduce-only、委託保證金與持倉保證金；每次成交後檢查 exposure 與新保證金。
- 已實現盈虧透過守恆 journal 入帳，未實現盈虧與 authoritative 餘額分開。帳戶沒有可支付來源時不能為贏方憑空產生可提領資產。
- 標記／指數價格須有具名後端資料來源、來源／接收時間、freshness、異常／斷線策略；Binance 現貨最後價不自動等於永續標記價格。
- 明確資金費率 interval、rate 計算／來源、cap、快照與冪等扣收；資金費率是產品必要能力，不能設定固定零後宣稱完整。
- 維持保證金、強平觸發、清算成交流動性、保險／穿倉責任與無流動性停機流程須形成可執行政策；不能只取消委託冒充強平完成。
- 對帳、恢復與行情失效時的減倉／風險處置必須先於客戶可開新倉。

驗收：開倉／加倉／減倉／平倉、費用、PnL、funding、限制槓桿、自成交、reduce-only 競態、價格失效、強平及穿倉路徑，均有受控可重現證據；沒有完整風險閉環不可開放客戶持倉。

### D4：客戶 UI、端到端與交付

- 重用共用版型、圖表與元件；現貨與合約表單改接各自 authoritative API，不混用 mock balance 或委託簿。
- 數量／名義額／保證金預估使用精確 decimal；server 回傳 authoritative fee／margin。等待、拒絕、部分成交、撤單失敗與 session 失效明確呈現。
- 不因資產修正成功就宣稱下單可用；完整顯示可用／鎖定／持倉資料，過期時不能繼續用舊資產預估送單。
- 同步 `docs/ai/LUMIX_UI_GOVERNANCE.md` 與 `LUMIX_UI_IMPLEMENTATION.md`，執行既有前端 typecheck／build、適當寬度及兩帳戶端到端驗收。
- 每個完整交付單位保留 HUMAN_REVIEW_REQUIRED、測試／回滾證據與獨立 commit／push；不把下一個未完成單位當成已交付。

## 必須明確的產品參數

已透過問題請求使用者確認：

- 現貨與永續由 LumiX 使用者互相撮合，或平台擔任對手盤。
- maker／taker 費率、最低費用與 rounding；若本機測試採零費率，需明確同意，且不代表正式 fee policy。
- 永續逐倉槓桿上限、維持保證金與風險限額、標記／指數價來源、資金費率及清算／穿倉政策。可先擬建議供審閱，但不得從 sandbox 數字自行提升為正式規則。

必要 runtime 測試可依本次任務授權直接新增：資金守恆、超支、冪等、partial fill／cancel race、owner scope、margin／PnL／funding／liquidation。既有 pure sandbox 測試不重複涵蓋這些；交易規格演進需同步維護預期結果。

## Schema、測試與回滾原則

已建立 V004__trading_execution.sql，依序套於 V001–V003 後；已在隔離 PostgreSQL 測試庫驗證，尚未對開發／正式資料庫套用。不變更已套用 baseline。不得以此次需求推定需要支援歷史 schema 升級或刪除現有物件。migration 順序為商品／版本化政策與執行關聯，再持倉／資金費率／清算 evidence，最後才開通相關命令；實際順序須依最終核定模型重新審查。

回滾先停止接新單、保留查詢／安全撤單與對帳；已有持倉時不得直接停掉風險監控。資料保留，ledger 僅能追加修正，不 DROP 訂單／成交／持倉或刪除使用者資金。新 schema 尚無業務資料時才可依核定 rollback 順序退回。

純規則單元測試可離線；MockMvc 驗證公開 API owner／冪等／錯誤契約；資金交易、鎖及原子性必須使用現有隔離 PostgreSQL 測試框架，不以 mocked JdbcTemplate 證明並行資金安全。既有 `JdbcDataSource` 會重建專用 `lumix_test` 測試 schema，因此執行前確認測試 endpoint，不能對使用者開發或正式 DB 操作。

不以目前設計文件表示完成下單。production readiness 與正式 launch 仍受權威 readiness gates 及人類簽核限制。

本次基線驗證（2026-11-21）：

```sh
./mvnw --offline '-Dtest=SpotSandboxOrderIntakePolicyTest,SpotSandboxMatchingPolicyTest,SpotSandboxSettlementPolicyTest,FuturesOrderPlacementGateTest,FuturesSandboxMatchingGateTest,IsolatedMarginCheckGateTest' test
```

歷史基線實際執行 32 項，0 failures／errors／skipped；僅證明既有 pure sandbox 規則。以下另記本次新增 runtime 證據；未修改使用者開發資料。

## 2026-11-21 實作與實際驗證紀錄

狀態：`SPOT_RUNTIME_AND_SHARED_UI_IMPLEMENTED_PENDING_POLICY_AND_END_TO_END`。`HUMAN_REVIEW_REQUIRED`。

- D1 已實作：獨立 instrument、不可變政策版本、canonical decimal／冪等、持久化委託序號、partial reservation、owner 查詢與命令來源 gate。現貨採政策明確指定的 CLIENT_TO_CLIENT；不預設實際費率、不預設啟用商品。交易先取全域 transaction advisory lock，再鎖受影響帳戶及 projection；ledger updater 先鎖 projection 才重算，避免等鎖前的舊 SUM 覆蓋新入帳。
- D2 後端已實作：LIMIT GTC／IOC／FOK、具明確 slippage 保護的 MARKET IOC／FOK、maker 價與價格／持久化序號優先、拒絕自成交、真實 base／quote／fee 守恆、找零釋放、owner 撤單。單命令最多 100 個對手單，超出預算的 GTC 全回滾；沒有外部參考簿成交。
- D3 未實作：永續 margin、position、PnL、funding、mark／index provider、liquidation／insurance／recovery。交易對手責任及具體政策待明確；目前 API 明示 FUTURES_RISK_RUNTIME_UNAVAILABLE，無開倉入口。
- D4 已接線但未完整驗收：兩種頁面重用 API 面板，顯示真實餘額／委託／成交／自有簿；前端沒有 mock balance 或本地假成功。外部圖表明示 Binance 現貨參考，不能當作永續 mark。仍待真實兩帳號、核定政策與資產的端到端驗收。

API：

- `GET /api/v1/trading/{kind}/orders`、`GET /{orderId}`：owner-only 委託分頁／明細。
- `GET /api/v1/trading/{kind}/workspace?market=BTC-USDT`：primary 一致快照的餘額、最近本人成交與 LumiX 聚合簿。無 projection 時回 null，不能假定有資金。
- `GET /api/v1/trading/{kind}/configuration?market=BTC-USDT`：後端能力及 decimal string 商品規格／費率。沒有完整政策時禁止新單。
- `POST /api/v1/trading/spot/orders`：最多 4096 bytes JSON，金額必須 JSON string，未知欄位拒絕，必須 `Idempotency-Key`、精確 Origin 與 `X-LumiX-Intent: trading-command`。
- `DELETE /api/v1/trading/spot/orders/{orderId}`：相同來源與冪等 gate；停止新單不阻擋安全撤單。
- `POST /api/admin/v1/trading/spot/policies`：最高管理員且精確 admin Origin／`X-LumiX-Intent: trading-policy`，最多 16384 bytes。完整政策逐版本追加，同版異內容拒絕，有 OPEN 單時不能換規格；同交易寫 ADMIN audit/outbox。必要時只建立空的具名收費帳戶，不產生資金。

啟用設定：`lumix.trading.spot-enabled` 預設 false；`lumix.trading.allowed-origin` 與 `lumix.trading.admin-allowed-origin` 預設空，缺值拒絕命令。政策發佈還需 existing ACTIVE market／assets、明確 CLIENT_TO_CLIENT、費率／取整／slippage／數量限制及收費帳戶。尚未核定產品政策，未啟用實際下單。

驗證：

- PostgreSQL 真實整合及 HTTP／數值單元測試共 31 個不同案例通過：現貨 7、partial reservation 10、query HTTP 3、command HTTP 4、decimal admission 4、ledger 1、internal transfer 2。最後整組 30 項通過後，新增的價格／時間優先案例與全部 7 項現貨測試再跑通過。fixture 僅在 test source set，不被 application 載入。
- `npm run typecheck`、`npm run build` 通過；既有 bundle size warning 仍在。
- `web/tests/trading-command-boundary.cjs` 使用僅存在測試 source 的 route fixture，驗證非確定送單同鍵重試、decimal string、1440／768／390px、手機完整表格捲動、合約 gate，零 page errors。這不是兩個真實帳戶的端到端成交證據。
- 暫時啟動前台做瀏覽器驗證，驗證後已停止；未啟動 API／admin，未啟用交易政策或存入開發資產。

回退：先關閉 spot-enabled；保留 owner 查詢與來源設定下的安全撤單。已有委託／成交的 V004 不可 DROP；ledger 修正只能 append。policy／trade execution evidence 禁止 UPDATE／DELETE。永續風險尚未啟用，沒有持倉可停止監控的暗示。

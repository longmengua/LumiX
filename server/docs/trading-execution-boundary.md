# 現貨與合約客戶下單實作邊界

日期：2026-11-21。`HUMAN_REVIEW_REQUIRED`。

使用者已要求完成現貨與合約客戶下單；本文件將該需求拆成真實交易 runtime 的施工相依，不將表單可點選或 sandbox 通過視為可交易。現階段完成程式盤點與施工設計，尚未修改交易 runtime、schema 或前端下單行為。交易對手／風險參數待確認；必要測試依本次任務已授權的 AI 施工流程豁免直接新增，不再等待人工批准。其他新任務是否適用豁免，依 AGENTS.md 的任務限定規則判斷。

## 現有可重用能力與缺口

| 範圍 | 已有程式證據 | 必須補齊的能力 |
| --- | --- | --- |
| 現貨頁 | `web/src/features/trading/SpotReferenceMarket.tsx`、`SpotTradingUnavailablePanels.tsx` | 表單仍停用；來源是 Binance 參考行情，不是 LumiX 可成交簿 |
| 合約頁 | `TradingWorkspace.tsx` 使用 `useTradingWorkspaceMock`；`TradingOrderForm.tsx` 只回報 savedLocally | 移除交易路徑的 mock 餘額、委託、持倉與假成功，改接真實 API |
| 訂單／成交 schema | `server/src/main/resources/db/migration/V001__baseline.sql` 的 orders、trades | 無實際下單／撤單 Controller 與交易寫入 service，亦無持倉／資金費率 runtime schema |
| 市場 schema | markets 的 base／quote 唯一，只有價格／數量 scale | 缺現貨與永續商品識別、交易規格、步進／最小名義額、版本化 fee／risk policy，不能以相同 underlying symbol 混合撮合 |
| 帳本 | `TransactionalLedgerPostingService`、`LedgerBalanceProjectionUpdater` | 重用 append-only journal 與 projection；驗證跨帳戶鎖定順序、重放及與 reservation 的原子性 |
| 資產保留 | `ReservationHoldService`、`ReservationCaptureService`、`ReservationReleaseService` | capture 目前消耗全部 remaining；實際部分成交必須按成交額扣用，並正確保留未成交及價格改善釋放額 |
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

目前未建立 migration。後續先對 current final schema 設計新增變更，不變更已套用 baseline。不得以此次需求推定需要支援歷史 schema 升級或刪除現有物件。migration 順序為商品／版本化政策與執行關聯，再持倉／資金費率／清算 evidence，最後才開通相關命令；實際順序須依最終核定模型重新審查。

回滾先停止接新單、保留查詢／安全撤單與對帳；已有持倉時不得直接停掉風險監控。資料保留，ledger 僅能追加修正，不 DROP 訂單／成交／持倉或刪除使用者資金。新 schema 尚無業務資料時才可依核定 rollback 順序退回。

純規則單元測試可離線；MockMvc 驗證公開 API owner／冪等／錯誤契約；資金交易、鎖及原子性必須使用現有隔離 PostgreSQL 測試框架，不以 mocked JdbcTemplate 證明並行資金安全。既有 `JdbcDataSource` 會重建專用 `lumix_test` 測試 schema，因此執行前確認測試 endpoint，不能對使用者開發或正式 DB 操作。

不以目前設計文件表示完成下單。production readiness 與正式 launch 仍受權威 readiness gates 及人類簽核限制。

本次基線驗證（2026-11-21）：

```sh
./mvnw --offline '-Dtest=SpotSandboxOrderIntakePolicyTest,SpotSandboxMatchingPolicyTest,SpotSandboxSettlementPolicyTest,FuturesOrderPlacementGateTest,FuturesSandboxMatchingGateTest,IsolatedMarginCheckGateTest' test
```

實際執行 32 項，0 failures／errors／skipped。僅證明既有 pure sandbox 規則基線，沒有驗證新 runtime、PostgreSQL 並行資金安全或客戶端到端下單。尚未新增測試，也未修改使用者資料。

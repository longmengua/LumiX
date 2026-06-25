<!-- 檔案用途：完整交易所功能驗證表；每列是一個可執行、可記錄證據的驗收項。 -->
# Exchange Function Validation

這份表用來驗證 Java21 Match Hub 作為內部交易所 MVP 的端到端功能。它不是 production certification；它是 release、demo、重構後回歸，以及新增功能前後對照的驗收矩陣。

## 填寫規則

| 欄位 | 說明 |
| --- | --- |
| ID | 穩定驗證項編號。 |
| Area | 功能區域。 |
| Scenario | 要驗證的使用者或系統情境。 |
| Expected | 通過標準。 |
| Suggested Evidence | 建議留下的證據。 |
| Status | `todo`、`pass`、`fail`、`blocked`、`n/a`。 |
| Notes | 補充環境、測試資料或缺口。 |

## Smoke Gate

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| SMK-001 | Startup | 啟動 MySQL、Redis、Kafka 與 Spring Boot app。 | App health 可用，啟動無 migration 或 bean error。 | `docker compose ps`、`./mvnw spring-boot:run` log、health response。 | todo | 本機預設 profile。 |
| SMK-002 | Static App | 開啟 exchange console。 | `exchange.html` 可載入，靜態 JS/CSS 無 404。 | Browser screenshot 或 Playwright screenshot。 | todo | 若已有舊 tab，需 reload。 |
| SMK-003 | Market Bootstrap | 前台從 `/api/markets` 載入市場。 | 只顯示 enabled market，包含 product type、base/quote、tick/lot/fee 資訊。 | `/api/markets` response。 | todo | 覆蓋 `BTCUSDT`、`BTCUSDT-PERP`、`BTCUSDT-SPOT`。 |
| SMK-004 | Basic Trade | 兩個帳戶完成一筆可成交訂單。 | 下單成功、成交生成、訂單狀態更新、帳務更新、行情更新。 | API response、ledger rows、order lifecycle、trade tape。 | todo | 建議先跑合約，再跑現貨。 |
| SMK-005 | WebSocket Push | 訂閱 `/ws/exchange` 後觸發下單或 quote。 | 收到 market/order/trade 或 quote 事件，斷線 fallback 不影響查詢。 | observed event names、payload sample。 | todo | 從同 origin 頁面驗證。 |

## Customer Auth And Frontend

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| AUTH-001 | Registration | 使用 email/password 註冊。 | 建立 pending registration，不立即建立可交易帳戶。 | `/api/auth/register` response、DB row。 | todo | 驗證碼與備援 link 都要可追蹤。 |
| AUTH-002 | Email Verification | 使用六位數驗證碼完成註冊。 | 建立 app user 與帳戶，pending request 完成。 | `/api/auth/verify-email` response。 | todo | 覆蓋過期與錯誤碼。 |
| AUTH-003 | Resend Verification | pending 註冊重寄驗證。 | code/link 輪換，但不延長原 24 小時期限。 | `/api/auth/resend-verification` response。 | todo | 檢查舊 code 不可用。 |
| AUTH-004 | Login Logout | 登入後查 `/api/auth/me`，再登出。 | session 可取得 uid，登出後 private API 不可用。 | `/api/auth/login`、`/api/auth/me`、`/api/auth/logout`。 | todo | 前台 UID 不可手動編輯。 |
| AUTH-005 | Language Preference | 切換 English、繁中、Bahasa Malaysia、韓文。 | 前台與做市商後台共用 `localStorage.exchangeLanguage`。 | UI screenshot、localStorage value。 | todo | English 是 first-load demo default。 |
| AUTH-006 | Human Verification | 啟用真人驗證 config。 | 註冊流程顯示 challenge，後端按 config 驗證。 | `/api/auth/config` response、註冊結果。 | todo | Turnstile-compatible。 |

## Market Discovery And Public Market Data

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| MKT-001 | Market List | 查詢公開市場列表。 | 回傳 enabled symbols，含 product type、fee、asset 與交易規則。 | `GET /api/markets`。 | todo | 不暴露 admin-only 診斷資料。 |
| MKT-002 | Depth Snapshot | 查詢 order book depth。 | bids/asks 排序正確，depth limit 生效。 | `GET /api/depth/{symbol}`。 | todo | 前台支援 5/10/20/50 ticks。 |
| MKT-003 | Depth Delta | 查詢 known version 後的 depth deltas。 | sequence/checksum 可用於 reconnect backfill。 | `/api/market-data/{symbol}/depth-deltas`。 | todo | 覆蓋 gap 與 no-new-delta。 |
| MKT-004 | Ticker | 查詢 ticker。 | top of book、last trade、mark/index 相關欄位符合最新狀態。 | `/api/market-data/{symbol}/ticker`。 | todo | 現貨/合約都要覆蓋。 |
| MKT-005 | Trades | 查詢 trade tape。 | cursor 可重放，排序穩定。 | `/api/market-data/{symbol}/trades`。 | todo | 覆蓋 `afterTs` / `afterMatchId`。 |
| MKT-006 | Kline | 查詢 kline。 | interval 聚合符合成交資料。 | `/api/market-data/{symbol}/klines`。 | todo | 無成交時回傳策略需記錄。 |
| MKT-007 | SSE Market Stream | 訂閱 public SSE。 | market event 正常推送，draining 時新 stream 受控。 | `/api/market-data/{symbol}/stream`。 | todo | 也可用 WebSocket 覆蓋。 |

## Order Entry And Matching

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| ORD-001 | Limit GTC | 送出不可立即成交 LIMIT GTC。 | 訂單 accepted 並 resting，預凍資金正確。 | `/api/order/place`、open orders、ledger。 | todo | BUY/SELL 都要覆蓋。 |
| ORD-002 | Immediate Match | 送出可成交 LIMIT。 | 價格時間優先成交，maker/taker lifecycle 正確。 | order lifecycle、trade event、projection。 | todo | 檢查 partial fill。 |
| ORD-003 | Market Order | 送出 MARKET order。 | 依簿內流動性成交，不足時按規則拒絕或部分處理。 | API response、trade tape。 | todo | 覆蓋空簿。 |
| ORD-004 | IOC | 送出 IOC。 | 可成交部分成交，剩餘立即取消並釋放預凍。 | lifecycle、ledger release。 | todo | |
| ORD-005 | FOK | 送出 FOK。 | 流動性不足時不成交；足夠時一次完成。 | lifecycle、trade count。 | todo | |
| ORD-006 | Post Only | 送出會吃單的 post-only。 | 訂單拒絕，不產生成交或預凍殘留。 | rejection code、ledger diff。 | todo | |
| ORD-007 | Self Match Prevention | 同 uid 自成交情境。 | 按 self-match prevention 規則處理，不產生錯誤帳務。 | order lifecycle、trade tape。 | todo | |
| ORD-008 | Amend Resting Order | 修改仍在簿內 LIMIT order。 | 不主動吃單，價格/剩餘 qty/clientOrderId 更新，預凍調整。 | `PATCH /api/order/{orderId}`、ledger。 | todo | |
| ORD-009 | Cancel Replace | cancel-replace open order。 | 原單取消、replacement 建立，client order id 去重仍有效。 | `POST /api/order/{orderId}/replace`。 | todo | |
| ORD-010 | Single Cancel | 取消 open order。 | 狀態 canceled，剩餘預凍釋放。 | `DELETE /api/order/{orderId}`。 | todo | |
| ORD-011 | Bulk Cancel | 取消 uid/symbol 或 uid 全部 open orders。 | 符合 filter 的 open orders 全部取消，其他 symbol 不受影響。 | `DELETE /api/order/open`。 | todo | |
| ORD-012 | Query Orders | 查 open/all/projections/lifecycle。 | open、history、projection 與 event log 一致。 | `/api/order/open`、`/api/order/all`、projection APIs。 | todo | |
| ORD-013 | Projection Rebuild | 用 lifecycle event 重建 projection。 | rebuild 後 projection 等於目前最新狀態。 | `POST /api/order/{orderId}/projection/rebuild`。 | todo | |
| ORD-014 | Client Order Id | 重送相同 clientOrderId。 | 去重或衝突處理穩定，不重複下單。 | API response、order count。 | todo | |
| ORD-015 | Frequency Limit | 啟用 uid+symbol 下單頻率限制。 | 超限拒絕，未超限通過。 | config、API response。 | todo | 預設 disabled 時記錄為 n/a。 |

## Product Accounting: Spot And Perpetual

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| PROD-001 | Perpetual Reserve | 合約 BUY/SELL 下單。 | 預凍 quote/margin asset 的 initial margin + taker fee buffer。 | account order hold、ledger posting。 | todo | |
| PROD-002 | Perpetual Position | 合約成交。 | 建立/更新 Position、position margin、realized PnL、fee/rebate。 | positions、ledger、risk snapshot。 | todo | |
| PROD-003 | Spot Buy Reserve | 現貨 BUY 下單。 | 預凍 quote notional + fee buffer。 | account quote order hold。 | todo | |
| PROD-004 | Spot Sell Reserve | 現貨 SELL 下單。 | 預凍 base quantity。 | account base order hold。 | todo | |
| PROD-005 | Spot Settlement | 現貨成交。 | BUY 收 base 扣 quote；SELL 扣 base 收 quote；不建立 Position。 | account balances、ledger、position query。 | todo | |
| PROD-006 | Spot Validation | 現貨使用 leverage != 1 或 reduce-only。 | 訂單拒絕，無預凍殘留。 | rejection response、account diff。 | todo | |
| PROD-007 | Symbol Separation | `BTCUSDT-PERP` 與 `BTCUSDT-SPOT` 同 base/quote。 | order book、risk、settlement 不互相污染。 | depth、orders、ledger by symbol。 | todo | |

## Funds, Wallet Ledger, And Account

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| FUND-001 | Deposit | 入金指定 asset。 | balance/available 增加，transfer confirmed，ledger balanced。 | `/api/margin/deposit`、account、ledger。 | todo | 覆蓋 default USDT 與非 USDT。 |
| FUND-002 | Deposit Callback Idempotency | 重放同一 externalRef。 | 相同 payload no-op；衝突 payload 拒絕。 | `/api/margin/deposit/callback`。 | todo | asset 也是 payload 的一部分。 |
| FUND-003 | Withdraw | 可用餘額足夠時出金。 | available 減少，transfer confirmed，ledger balanced。 | `/api/margin/withdraw`。 | todo | |
| FUND-004 | Withdraw Insufficient | 可用餘額不足時出金。 | transfer failed，不寫扣款 ledger。 | transfer status、ledger count。 | todo | |
| FUND-005 | Transfer | 全倉/逐倉或帳戶內轉帳。 | margin mode/account state 符合預期。 | `/api/margin/transfer`。 | todo | 若功能不適用標 n/a。 |
| FUND-006 | Account Query | 查帳戶資產。 | per-asset balance/available/orderHold 與 legacy cross view 一致。 | `/api/margin/account`。 | todo | |
| FUND-007 | Ledger Query | 查 wallet ledger。 | entries/postings 成對、方向與 account code 正確。 | `/api/margin/ledger`。 | todo | |
| FUND-008 | Ledger Replay | 重放 ledger。 | replay balance 等於 account balance。 | `/api/margin/ledger/replay`。 | todo | |
| FUND-009 | Ledger Compare | ledger replay compare。 | 無差異或差異可定位。 | `/api/margin/ledger/replay/compare`。 | todo | |
| FUND-010 | Transfers Query | 查 transfer 列表與 reconciliation。 | deposit/withdraw/manual-review 狀態可追蹤。 | `/api/margin/transfers`、reconciliation APIs。 | todo | |
| FUND-011 | Bonus Credit | 體驗金 grant/consume/expire/clawback。 | 不混入真實現金餘額，流水與報表可查。 | bonus report/export/clawback APIs。 | todo | |
| FUND-012 | Turnover | 成交後產生流水 facts。 | summary、records、export、reconciliation 與 trade tape 對齊。 | `/api/margin/turnover/*`。 | todo | |

## Risk, Funding, Liquidation, And ADL

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| RISK-001 | Pre-Trade Rules | tick、lot、min notional、price band、max order size。 | 不合規訂單拒絕，原因碼穩定。 | order rejection tests/API。 | todo | |
| RISK-002 | Balance Check | 下單超過可用餘額。 | 拒絕且無 ledger hold。 | API response、account diff。 | todo | |
| RISK-003 | Leverage Limit | 合約槓桿與 risk tier。 | 超限拒絕，合法訂單 margin reserve 正確。 | risk config、order response。 | todo | |
| RISK-004 | Reduce Only | 合約 reduce-only。 | 只能減倉，不可增加 exposure。 | position before/after。 | todo | |
| RISK-005 | Position Limit | position notional limit。 | 超限拒絕，未超限通過。 | risk snapshot、order response。 | todo | |
| RISK-006 | Risk Snapshot | 查詢與持久化 risk snapshot。 | equity、margin、risk ratio、open position count 正確。 | `/api/margin/risk*`。 | todo | |
| RISK-007 | Price Oracle | 更新與查詢 mark/index price。 | risk、funding、liquidation 使用最新 oracle。 | `/api/risk/price-oracle`。 | todo | |
| RISK-008 | Funding Settlement | 執行 funding settlement。 | 多空資金費帳務、event、risk snapshot 正確。 | `/api/risk/funding/settle`。 | todo | |
| RISK-009 | Manual Liquidation | 手動 liquidation。 | position close、shortfall/insurance/ADL routing、audit event 正確。 | `/api/risk/liquidate`、ledger、audit。 | todo | |
| RISK-010 | Liquidation Controls | halt/manual review 開關。 | halt 時不執行；manual review 產生待審決策。 | config、decision audit。 | todo | |
| RISK-011 | Liquidation Scan | 掃描 open positions。 | 可清算倉位被 route，不可清算倉位不誤殺。 | scheduler/service test 或 API evidence。 | todo | |
| RISK-012 | Insurance Fund | 查詢 insurance fund 與 movements。 | shortfall/payout movement 可追蹤。 | `/api/risk/insurance-fund*`。 | todo | |
| RISK-013 | ADL Queue | liquidation shortfall 進 ADL queue。 | idempotent enqueue，duplicate 不覆蓋 claim。 | `/api/risk/adl-queue`。 | todo | |
| RISK-014 | ADL Claim Release | operator claim/release。 | owner guard 生效，stuck claim 可報告。 | claim/release APIs。 | todo | |
| RISK-015 | ADL Execute | 執行 ADL。 | candidate ranking、partial retry、no-candidate retry、ledger 正確。 | `/api/risk/adl-queue/{id}/execute`。 | todo | |
| RISK-016 | ADL Reconciliation | ADL 與 insurance reconciliation。 | 不平衡或殘留 shortfall 可報告。 | `/api/risk/adl-insurance-reconciliation`。 | todo | |

## WebSocket, SSE, And Push Recovery

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| PUSH-001 | Public Subscribe | WebSocket subscribe market。 | 收到 `subscribed.market` 與後續 public market events。 | observed events。 | todo | 同 origin 驗證。 |
| PUSH-002 | Private Subscribe | 登入後 subscribe user。 | 只能訂閱自己 uid，收到 order/trade/account events。 | observed events、auth evidence。 | todo | |
| PUSH-003 | Quote Push | 做市商 quote 觸發推送。 | 收到 `market-maker.quote` 或對應 quote event。 | quote ref id、event payload。 | todo | |
| PUSH-004 | Disconnect Fallback | WebSocket 斷線。 | 前台 fallback 到 1 秒 polling；重連後恢復 push。 | browser log、network trace。 | todo | |
| PUSH-005 | Cancel On Disconnect | 啟用 cancel-on-disconnect。 | 斷線後符合條件的 open orders 被取消，可 resume metadata。 | lifecycle、cancel reason。 | todo | |
| PUSH-006 | Recovery Cursor | client 用 cursor 補資料。 | depth/trade replay 可補斷線期間事件。 | recovery cursor APIs。 | todo | |
| PUSH-007 | Gateway Draining | push gateway draining。 | 新連線 503，既有 stream 可 drain。 | `/api/ops/push-gateway/status`、stream behavior。 | todo | |

## Admin And Operator Console

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| ADM-001 | Market Config Read | admin 查市場配置。 | 回傳 symbols、fees、trading rules、diagnostics。 | `/api/admin/market-config`。 | todo | |
| ADM-002 | Market Fee Update | admin 更新 fee。 | fee change 生效且有 change history。 | `POST /api/admin/market-config/{symbol}/fees`。 | todo | |
| ADM-003 | Trading Rules Update | admin 更新 tick/lot/min notional 等。 | 新規則影響後續下單。 | `POST /api/admin/market-config/{symbol}/trading-rules`。 | todo | |
| ADM-004 | Risk Parameters Read | admin 查風控參數。 | risk tier / global switch 可讀。 | `/api/admin/risk-parameters`。 | todo | |
| ADM-005 | DLQ Read | admin 查 DLQ。 | payload/header preview 遮罩敏感資訊。 | `/api/admin/dlq`。 | todo | |
| ADM-006 | Test Funds | admin 發測試金。 | 測試金只經 admin test-funds path，前台不能自發。 | `/api/admin/test-funds/airdrop`。 | todo | |
| ADM-007 | Admin Page Separation | 前台不暴露 admin/market-maker privileged links。 | customer UI 沒有後台入口。 | screenshot、DOM search。 | todo | |
| ADM-008 | Security Classifier | protected API 需要正確 credential/scope。 | 未授權 401/403，授權通過。 | security tests/API evidence。 | todo | |

## Market Maker And Hedging

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| MM-001 | Profile Create Update | 建立/更新 market-maker profile。 | profile、per-symbol limits、enabled 狀態可查。 | `/api/market-maker/profiles`。 | todo | |
| MM-002 | Quote Submit | 送出雙邊或 ladder quote。 | quote replacement、risk limit、post-only checks 生效。 | `/api/market-maker/quotes`、active quotes。 | todo | |
| MM-003 | Quote Rate Limit | quote API rate limit。 | 超限拒絕且沒有 replacement side effect。 | rate-limit config、response。 | todo | |
| MM-004 | Quote Reconciliation | 查 quote reconciliation。 | stale/missing state 可報告，可 repair。 | quote reconciliation APIs。 | todo | |
| MM-005 | Auto Quote | run-once 或 scheduler。 | fair value/spread/fee/hedge cost 邏輯產生 quotes。 | `/api/market-maker/auto-quote/*`。 | todo | 預設 scheduler disabled。 |
| MM-006 | Exposure Query | 查 inventory exposure/skew。 | exposure 與成交後 position/account 對齊。 | `/api/market-maker/profiles/{id}/exposures`。 | todo | |
| MM-007 | Hedge Execution | 手動 hedge execution。 | approval、route caps、halt controls、audit trail 生效。 | hedge execution APIs、audit rows。 | todo | |
| MM-008 | Hedge Callback | venue callback ingestion。 | HMAC/timestamp 驗證、fill persistence、idempotency 正確。 | `/api/market-maker/hedge-fills/venue-callback`。 | todo | |
| MM-009 | Hedge Reconciliation | hedge fill/reconciliation 查詢。 | venue order/ref id 查詢與 unresolved idempotency 可用。 | hedge fill/reconciliation APIs。 | todo | |
| MM-010 | Operator UI | `admin-market-maker.html`。 | profile、limits、quotes、hedge、idempotency 面板可操作。 | screenshot、API trace。 | todo | |

## Recovery, Reconciliation, And Finance Operations

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| REC-001 | Account Recovery | recover/validate account state。 | Redis/account state 可恢復並驗證一致。 | `/api/recovery/recover/{uid}`、validate API。 | todo | |
| REC-002 | Account Reconciliation | account reconciliation report。 | mismatch 可報告並持久化。 | `/api/recovery/reconcile/accounts*`。 | todo | |
| REC-003 | Account Position Consistency | restore 後 account/position 對齊。 | missing account 或 margin shortage 可報告。 | `/api/recovery/restore/account-position-consistency`。 | todo | |
| REC-004 | Ledger Tamper Evidence | ledger hash-chain 檢查。 | 篡改或缺口可定位。 | `/api/recovery/reconcile/ledger/tamper-evidence`。 | todo | |
| REC-005 | Trial Balance | trial balance snapshot。 | debit/credit by asset/account code 平衡。 | `/api/recovery/finance/trial-balance/*`。 | todo | |
| REC-006 | Daily Finance Report | daily finance report/category report/export。 | fee/funding/liquidation/bonus/transfer 分類可查。 | finance APIs。 | todo | |
| REC-007 | Archive Eligibility | ledger archive eligibility/manifest/restore/replay/delete guard。 | archive 前置條件與 restore smoke 可驗證。 | archive APIs。 | todo | |
| REC-008 | Reconcile Issue Workflow | claim/resolve/reopen issue。 | owner/status/resolved_at/audit event 正確。 | issue workflow APIs。 | todo | |
| REC-009 | Outbox DLQ | outbox dead replay/compensate。 | replay/compensation 有 guard 與結果記錄。 | `/api/recovery/outbox/*`。 | todo | |
| REC-010 | Domain-State Consistency | MySQL/Redis/Kafka consistency report。 | outbox/domain state 差異可報告。 | `/api/recovery/outbox/domain-state-consistency`。 | todo | |
| REC-011 | Matching Worker Context | 查 worker context。 | owner/epoch/symbol 狀態可見。 | `/api/recovery/matching-worker/contexts`。 | todo | |

## Polymarket Integration

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| POLY-001 | Market Discover | Gamma market discovery。 | market rows 可建立/更新，schema drift 可報告。 | `/api/prediction/markets/discover`。 | todo | |
| POLY-002 | Market Sync | sync/reset/progress/retry。 | sync checkpoint、retry、price refresh 正常。 | prediction market APIs。 | todo | |
| POLY-003 | CLOB API Key | create/derive CLOB API key。 | credential lifecycle 與安全 guard 生效。 | `/api/prediction/clob/api-key/*`。 | todo | |
| POLY-004 | Session Lifecycle | init/confirm/list/revoke/expire session。 | expired/revoked session 不可再用，abnormal use 有 audit warning。 | session APIs。 | todo | |
| POLY-005 | Place Order | 下 Polymarket order。 | local clientRequestId idempotency，CLOB response 持久化。 | `/api/prediction/orders`。 | todo | |
| POLY-006 | Local Order Query | 查 local orders。 | local/CLOB/trade/settlement state 可見。 | `/api/prediction/orders/local*`。 | todo | |
| POLY-007 | Cancel Order | cancel local/CLOB order。 | durable commandId、uncertain cancel replay/reconcile 正確。 | cancel/reconcile APIs。 | todo | |
| POLY-008 | Trade Events | user-channel trade event。 | matched lifecycle、lastTradeId、settlement/redeem transition 正確。 | trades API、local order projection。 | todo | |
| POLY-009 | User WS Worker | start/stop/status/replay。 | wallet-scoped checkpoint/replay 生效。 | `/api/prediction/ws/user/*`。 | todo | |
| POLY-010 | Approvals | allowance/status/cache。 | TTL cache 與 delete cache 可用。 | approve APIs。 | todo | |
| POLY-011 | RPC Tracking | unresolved RPC transaction report。 | command/txHash/status envelope 可查。 | `/api/prediction/approve/rpc-transactions/unresolved`。 | todo | |

## Message Center

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| MSG-001 | User Messages | list/detail/read/read-all。 | pagination/filter/unread count 正確。 | `/api/messages*`。 | todo | |
| MSG-002 | User Actions | archive/unarchive/pin/delete。 | 狀態轉換與查詢一致。 | message action APIs。 | todo | |
| MSG-003 | Preferences | read/update preferences。 | preference 影響後續訊息策略。 | `/api/message-preferences`。 | todo | |
| MSG-004 | Admin Announcement | create/cancel/list announcement。 | 目標用戶可收到，取消後不再投遞。 | `/api/admin/messages/announcements*`。 | todo | |
| MSG-005 | System Message | send event/batch event。 | template/render/channel/recipient 正確。 | `/api/system/messages/*`。 | todo | |

## Observability And Operations

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| OPS-001 | Metrics | 查 `/api/ops/metrics`。 | app/order/matching/ledger/recovery 指標可讀。 | metrics response。 | todo | |
| OPS-002 | Push Gateway Status | 查 push gateway runtime。 | role、instance、readiness、draining 狀態正確。 | `/api/ops/push-gateway/status`。 | todo | |
| OPS-003 | Structured Logs | 下單、風控、reconciliation 產生 structured log。 | uid/orderId/clientOrderId/symbol/correlation id 可搜尋。 | log sample。 | todo | |
| OPS-004 | Tracing | 啟用 tracing export。 | critical flow trace 可在 collector/dashboard 查到。 | trace id、dashboard screenshot。 | todo | 預設 disabled 時標 n/a。 |
| OPS-005 | Alerts | matching halt、Kafka lag、DLQ、不平衡等 alert rule。 | signal/threshold/route/runbook 定義可驗證。 | alert rule doc、dispatch test。 | todo | |
| OPS-006 | Failure Drill | cross-store failure drill。 | failure 後 recovery report 可定位一致性問題。 | runbook record。 | todo | |

## Security And Abuse Controls

| ID | Area | Scenario | Expected | Suggested Evidence | Status | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| SEC-001 | Customer API Auth | customer private APIs。 | 未登入不可用，登入後只能操作自己資源。 | 401/403/200 matrix。 | todo | |
| SEC-002 | Admin API Auth | admin/recovery/risk/market-maker APIs。 | 需要 admin credential/scope。 | Protected API classifier tests。 | todo | |
| SEC-003 | Sensitive Data Masking | logs/API preview。 | secrets、tokens、headers、payload sensitive fields 被遮罩。 | log/API sample。 | todo | |
| SEC-004 | External Callback Verification | hedge callback 等外部回呼。 | timestamp/signature/idempotency 生效。 | callback tests/API。 | todo | |
| SEC-005 | Rate Limits | quote、hedge、stream、order entry rate limits。 | 超限不產生副作用。 | rate-limit response、state diff。 | todo | |
| SEC-006 | Secrets | SMTP、CLOB、signer、alert webhook。 | secrets 只從 env/secret manager 進入，不提交 config。 | config review、git scan。 | todo | |

## Suggested Release Evidence Bundle

每次完整驗證建議至少留下：

- commit hash、branch、profile、資料庫 migration 版本。
- `./shells/ai-context.sh` output summary。
- `./mvnw test` 或 focused tests 清單。
- API/curl 或 Playwright 記錄。
- WebSocket observed event names 與一筆 payload sample。
- 對帳/ledger/trial balance 結果。
- 已知 `fail` / `blocked` 項目與下一步 owner。

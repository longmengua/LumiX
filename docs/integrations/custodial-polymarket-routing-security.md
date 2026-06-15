# 代管式 Polymarket 路由與金鑰安全

本文定義目標架構：使用者不需要連接 EOA 錢包，也不接觸 Polymarket 金鑰；使用者只在本交易所帳戶內下單，由平台後端代管 Polymarket 簽名、送單、成交回補、帳務與風控。

## 產品模型

使用者只登入本交易所，透過交易所 UI / API 下單，並在交易所內查看餘額、訂單、成交與部位。

Polymarket 是後端 execution venue。平台負責：

- wallet / signer 管理。
- CLOB API authentication。
- order payload signing。
- external order routing。
- user-channel / polling reconciliation。
- 內部帳務與風控。

前端不得取得：

- EOA private key。
- Polymarket CLOB API secret / passphrase。
- 長效 privileged API key。
- 原始 signed Polymarket order payload，除非是明確的唯讀稽核用途。

## Polymarket 外部模型

Polymarket CLOB authentication 使用：

- L1 private key：用於建立/derive API credentials，以及簽 order payload。
- L2 API key / secret / passphrase：用於 authenticated CLOB request。
- 新 API user 建議使用 deposit wallet / `POLY_1271`，funder 是 deposit wallet address。

官方參考：<https://docs.polymarket.com/api-reference/authentication>

## 目標流程

```text
User
  -> Exchange UI / API
  -> OrderController / prediction order entry
  -> internal risk and balance freeze
  -> VenueRoutingService
  -> PolymarketSigningService
  -> PolymarketClobTradingClient
  -> Polymarket CLOB
  -> user channel / polling reconciliation
  -> internal order lifecycle, position, wallet ledger
```

## 金鑰分類

正式環境要把 key class 拆開，降低 blast radius：

- Cold treasury key：離線、HSM 或 MPC；只做大額資金移動與 hot signer 補資。
- Hot execution signer key：線上但限權，只簽 Polymarket order；必須有小額 venue exposure limit 與快速 rotation。
- Deposit wallet / funder address：Polymarket funder，搭配 `signatureType=3` / `POLY_1271`。
- CLOB L2 credentials：`apiKey`、`apiSecret`、`apiPassphrase`。
- RPC provider key：只用於 Polygon RPC，不可和交易 signer 混用。
- Internal auth keys：使用者 JWT/API key 與 service-to-service credential。

## Secret Storage

正式環境必須使用 secret manager、HSM、KMS 或 MPC。Application config 只能放 secret reference，不放 raw secret。

允許的 production config 形狀：

```yaml
polymarket:
  wallet:
    signer-ref: polymarket/prod/hot-signer-v1
    funder-address: ${POLYMARKET_WALLET_FUNDER_ADDRESS}
    signature-type: 3
  clob:
    credential-ref: polymarket/prod/clob-l2-v1
```

禁止：

- 把 private key 或 CLOB secret commit 到 Git。
- 在 SQL 明文保存 private key。
- 把 CLOB secret 回傳前端。
- dev/staging/prod 共用 credential。

## Signing Service 邊界

交易服務不可直接讀 private key。交易服務只能把受限 payload 交給專用 signing service：

```text
PolymarketSigningService.signOrder(unsignedOrder, policyContext)
```

Signing service 必須檢查：

- chain ID 是 Polygon mainnet `137`，除非明確是非 production 環境。
- payload type 是允許的 Polymarket order 或 CLOB auth payload。
- tokenId / market 已在本地 market config 啟用。
- 單筆、單 market、單 user、每日 venue notional limit。
- idempotency key 存在。
- request 帶 trace ID、actor、reason。
- 簽名決策寫入 append-only audit storage。

## Order Routing 規則

內部 order routing 必須 async 且可 replay：

1. 只有 internal risk 與 balance check 通過後才接受使用者訂單。
2. 先 freeze internal funds / margin，再 route 外部 venue。
3. 寫入 internal order 與 routing command，包含 idempotency key。
4. Worker 讀 routing command，建立 unsigned Polymarket order。
5. Signing service 根據 policy 決定是否簽名。
6. CLOB client 用 L2 headers 送出 signed order。
7. 保存 external order ID、remote status、request/response fingerprint。
8. user channel event 或 polling 更新 local lifecycle。
9. reconciliation 比對 local state、CLOB state、trades 與 internal ledger。

Controller 不應同步持有 private key，也不應直接卡在外部簽名與送單流程。

## 稽核與控制

真實資金前必須具備：

- 全域 Polymarket routing kill switch。
- 單 market disable switch。
- Hot signer notional caps。
- CLOB credential rotation runbook。
- Secret access audit。
- Signed-order audit trail。
- Remote fills 與 local ledger 不一致時告警。
- 使用者 route 到 Polymarket-backed market 前的 geo / KYC / eligibility gate。
- Polymarket open orders emergency cancel-all 流程。

## 本機開發

本機只能使用 dummy keys。如果任何真實 key 或 CLOB credential 曾經放進本機 ignored file，正式使用前必須 rotate。

`application-dev.yml` 與 `application-prod.yml` 必須持續使用環境變數或 secret reference：

- `POLYMARKET_WALLET_PRIVATE_KEY`
- `POLYMARKET_WALLET_FUNDER_ADDRESS`
- `POLYMARKET_CLOB_API_KEY`
- `POLYMARKET_CLOB_API_SECRET`
- `POLYMARKET_CLOB_API_PASSPHRASE`
- `POLYMARKET_RELAYER_API_KEY`
- `WEB3_POLYGON_RPC_URL`

## 第一批實作切片

1. 新增 secret-reference config model，production 啟動時拒絕 raw key material。
2. 新增 `PolymarketSigningService` interface，提供 local dev implementation 與 secret-manager/HSM adapter boundary。
3. 把 Polymarket order placement 放到 async venue-routing command 後面。
4. 新增 signing policy checks 與 audit records。
5. 新增 operator kill switch 與 per-market routing controls。
6. 新增 external Polymarket order state 與 internal order/ledger 的 reconciliation checks。

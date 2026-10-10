# Ethereum Wallet Service — 第一階段

## 支援範圍

只支援 Ethereum Mainnet（chainId `1`）、原生 ETH 與官方 ERC-20 USDT。USDT 合約為 `0xdAC17F958D2ee523a2206206994597C13D831ec7`，精度為 6。其他 token、其他鏈與前端傳入的 contract address 一律不被接受。

## Finality 與 reorg

系統以 `eth_getBlockByNumber("finalized", false)` 的 finalized checkpoint 判定 finality；不是以最新高度減固定確認數。每筆 observation 保存 block number/hash，未 finalized 前發現 canonical hash 不同即標記 `REORGED`，保留證據且不得 credit。

## 設定

```text
LUMIX_ETHEREUM_OBSERVATION_ENABLED=false
LUMIX_ETHEREUM_RPC_URL=<由部署 secret 注入>
LUMIX_ETHEREUM_CHAIN_ID=1
LUMIX_WITHDRAWAL_ENABLED=false
```

Kubernetes 必須以 Secret 提供 `LUMIX_ETHEREUM_RPC_URL`，不可寫進 values、ConfigMap、log 或 Git。

## 限制

- 第一階段不啟用真實提款簽章、廣播或 custody API；application 不保存 raw private key。
- 第一階段只承諾直接 ETH transaction 的偵測；不保證偵測 ETH internal contract transfer。
- USDT 的未來提款不得把 ERC-20 `transfer` 回傳 Boolean 當作成功依據；必須以交易 receipt、receipt status 與 finalized chain evidence 判定。

## 提款啟用前置條件

`LUMIX_WITHDRAWAL_ENABLED` 預設為 `false`；設定為 `true` 本身不會啟用提款。目前的 `DisabledCustodyProvider` 會拒絕全部執行，避免漏配部署時意外移動資金。

要提供真實提款，必須先完成一個具名 custody、MPC 或 HSM provider 的獨立整合，且至少具備：provider 帳戶／環境授權、network 與 asset allowlist、request-to-provider idempotency、immutable audit reference、timeout／retry 邊界、broadcast receipt、receipt status、finalized confirmation、reconciliation 與失敗即拒絕。application 不得接收、保存或使用 raw private key、mnemonic 或 seed phrase。

# P22-R01 — Ethereum Mainnet 入金觀測 Runtime

## 範圍

首發網路固定為 Ethereum Mainnet（chainId `1`），資產只允許原生 `ETH` 與官方 ERC-20 `USDT`。USDT 合約固定為 `0xdAC17F958D2ee523a2206206994597C13D831ec7`、精度為 6；任何 request、設定或 browser 輸入均不得指定其他 token contract。

本 task 先實作 provider-neutral 的 secret-safe RPC boundary、資產白名單、`finalized` 判定與持久化 schema。RPC 預設停用，只有由部署 secret 注入 `LUMIX_ETHEREUM_RPC_URL` 並顯式設定 `LUMIX_ETHEREUM_OBSERVATION_ENABLED=true` 時，infrastructure profile 才會建立 Web3j client。

## 不變式

- `eth_getBlockByNumber("finalized", false)` 對應的 finalized block 是唯一可進入後續 P23 candidate 的 finality 依據；不得以 `latest - 12` 取代。
- deposit block hash 和 canonical block hash 不同時必須標記 reorg，不能 finalise 或 credit。
- `ethereum_deposit_events` 使用 database unique constraint 保護 ETH 的 `(network, tx_hash, to_address)` 與 event 的 `(network, tx_hash, log_index)`；application replay 不是唯一保證。
- scanner、finality 與 P22 persistence 絕不 append ledger、更新 balance 或執行 credit。
- 所有 RPC URL、API key 與任何未來 custody secret 均不得出現在 log、程式碼或 Git。

## Schema 與 rollback

目前尚未上線，Ethereum observation schema 已併入唯一的 `V001__baseline.sql`，包含 `ethereum_deposit_events` 與 `block_scan_checkpoints`。本機資料庫必須清空後重新初始化，不保留舊 Flyway history 或資料。日後一旦 schema 已共享或上線，所有變更都必須恢復使用新的 append-only Flyway migration；不得再改寫 V001。

## 明確未完成

- address 派發與 ownership persistence；RPC block range adapter、transaction/log fetch 與 database scanner orchestration。
- scan checkpoint lock、RPC retry/backoff、metrics、reorg persistence 與 read-only reconciliation runtime。
- P23 ledger credit/reversal、P24 hold、P25 custody signer／broadcast；沒有任何真實提款、簽章、private key 或 raw key 設定。
- ETH internal contract transfer 偵測。第一階段僅承諾直接 ETH transaction；未引入 `debug_traceTransaction`。

## 目前實作進度

`EthereumEthDepositScanner` 只接受 managed address 的正值直接 ETH transaction，沒有 `to` 或 value 為零時不產生 candidate。`EthereumUsdtTransferLog` 固定驗證官方 USDT contract、ERC-20 `Transfer` topic、三個 indexed topic 與 uint256 data；`EthereumUsdtDepositScanner` 再以 managed address ownership 決定是否產生 candidate。兩者只回傳 immutable observation candidate，沒有資料庫寫入、credit、ledger 或餘額 mutation。USDT identity 保留 `(txHash, logIndex)`，讓同一 transaction 的不同 event 可被獨立觀測。

`HUMAN_REVIEW_REQUIRED: yes`，因為這是未來入金 credit 的 chain evidence 入口，但本 task 本身尚不產生資金異動。

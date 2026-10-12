# Sepolia 提款手動 E2E

此程序僅限受控測試環境，絕不使用 Mainnet key 或資金。

1. 設定受限的 `LUMIX_ETHEREUM_RPC_URL`、`LUMIX_ETHEREUM_CHAIN_ID=11155111`、`LUMIX_WITHDRAWAL_ENABLED=true`。
2. 啟動 signer 時選擇 `sepolia` profile，設定 encrypted keystore 路徑與密碼環境變數；不得把 key 寫入文件或 application YAML。
3. 對 signer 設定可驗證的 Sepolia source wallet、single/daily limit、允許資產及 test USDT contract。
4. 以已認證 session 建立地址，等待 configured cooldown 成為 ACTIVE。
5. 呼叫 `POST /api/v1/withdrawals`，附 `Idempotency-Key`；確認回應不是 COMPLETED。
6. 等待 risk/review、final risk、sign worker 和 broadcast worker；用 tx hash 查 receipt 與 finalized block。
7. 只有 reconciliation 完成 reservation capture 與 ledger post 後，狀態才可為 COMPLETED。

RPC timeout 時不得直接重簽；worker 先以既有 raw transaction hash/receipt 進行 reconciliation。

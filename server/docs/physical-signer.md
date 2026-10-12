# 實體簽章器邊界

`server/signer` 是獨立 Spring Boot application。它不連主 DB、不讀 user session、不做 review、不做 ledger，唯一責任是驗證受限 signing request 並回傳 signed raw transaction。

```text
Wallet Server --受控 HTTP capability--> Signer
Signer --加密 keystore（僅 dev/test/sepolia）--> signed transaction
Wallet Server --RPC--> Ethereum
```

Signer 對 withdrawal ID 與 intent hash 保存 durable idempotency。相同 pairing 回傳既有結果；同一 withdrawal ID 的不同 hash 會被拒絕。Signer 使用 immutable `amountAtomic`（而非 ERC-20 transaction 的零 ETH value）套用每筆與 UTC 日累積額度，並以獨立 policy row lock 序列化同資產的日額度判斷與寫入。它也會驗證 configured source wallet、ETH value/data 與 USDT contract/selector/calldata amount 的 transaction semantics。production/prod profile 或 chain ID 1 均會在啟動時拒絕 local signer。

目前 production signer provider 是 disabled placeholder；沒有 mTLS/HSM provider 時 production signer 不可啟動。

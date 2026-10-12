# 提款平台架構

## 範圍與安全邊界

本文件描述 Sepolia 可執行提款 runtime。Ethereum Mainnet 簽章在目前版本保持 fail-closed；主 application 不持有 raw private key、mnemonic 或 seed phrase。

```text
已認證使用者
  -> WithdrawalController
  -> WithdrawalOrchestrator
  -> reservation hold / risk / review
  -> final risk gate
  -> signer gateway
  -> 獨立 lumix-signer（僅 Sepolia profile）
  -> Ethereum RPC broadcast
  -> receipt / finalized reconciliation
  -> reservation capture + immutable ledger journal
  -> COMPLETED
```

`withdrawals` 是提款的唯一主表。地址簿、風控、review、簽章和鏈上交易各自只保存該主表的外鍵證據，不建立第二套提款主表。

## 不變量

- create 接受 body `idempotencyKey` 或既有 `Idempotency-Key` header；若兩者同時存在必須完全一致。durable idempotency key 與 request fingerprint 必須同時一致才能 replay。
- pre-broadcast 的 reject、cancel 或 failure 必須以 idempotent reservation release 解鎖資金。
- only receipt success plus finalized evidence 可進入 ledger settlement。
- chain failure 會保留失敗與 gas accounting evidence，絕不宣稱 completed。

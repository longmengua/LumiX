# 提款完成後的錢包架構快照

原有 legacy `DefaultWithdrawService` 已標記 deprecated，不能作為 production runtime。新 flow 重用既有 reservation、ledger、audit、outbox、session auth 和 Ethereum RPC abstraction。

```text
withdrawals（唯一主表）
  +-- withdrawal_addresses
  +-- withdrawal_risk_assessments / triggers
  +-- withdrawal_reviews
  +-- withdrawal_signing_requests
  +-- withdrawal_chain_transactions
```

此快照不表示 production ready。Mainnet signing、HSM/mTLS 與正式營運證據仍是 readiness gate 的未完成項目。

# 提款風控引擎

風控引擎以獨立 `WithdrawalRiskRule` 聚合為 `ALLOW`、`MANUAL_REVIEW`、`HOLD` 或 `DENY`。任一依賴不可用均不會預設放行。

已保存的 evidence 僅含 rule code、score 與去敏結果；不得保存 password、cookie、session secret、token 或任何 key material。

```text
凍結帳戶 / transfer restriction -> DENY
密碼剛變更 / 新裝置 / 新 IP -> MANUAL_REVIEW
Redis velocity 不可用 -> MANUAL_REVIEW
高額或 velocity threshold -> MANUAL_REVIEW
```

Redis 只提供 10 分鐘、1 小時、24 小時的 count/amount signal，資料庫 withdrawal records 才是財務事實來源。

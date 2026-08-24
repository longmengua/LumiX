# Phase 22 - 入金地址與鏈上監聽器

## 狀態

```text
IN_PROGRESS_P22_T03_COMPLETED
```

P22-T01 到 P22-T03 已完成 immutable address ownership、chain observation 與 finality/health contract；runtime、chain connection、secret、schema 與 credit 尚未開始。逐卡 approve 機制依人類指示暫停。

## 目標與依賴

建立 provider/chain-neutral 的入金地址所有權與鏈上觀測 foundation。施工必須先完成並通過 P21 review，並沿用 P13 identity/asset、P14 immutable ledger 的既有邊界；本 phase 不會 credit 資產。

## 詳細 task draft

| Draft | 目標與交付 | 禁止事項與驗收 |
| --- | --- | --- |
| P22-T01 | 定義 network、asset、address、ownership、address lifecycle 與唯一性契約 | COMPLETED；不產生地址、不寫 schema；已測試 network/address normalization、重複 ownership、錯誤格式 |
| P22-T02 | 定義 provider-neutral chain observation、block/transaction/log identity、cursor 與 finality observation contract | COMPLETED；不連 RPC、不存 secret；已測試 duplicate observation、錯鏈、replay 與 deterministic ordering |
| P22-T03 | 定義 reorg、confirmation、orphan、halt/resume health state 與通知邊界 | COMPLETED；不 credit、不調 balance/ledger；已測試 reorg、confirmation regression、gap/stale、multi-network 隔離 |
| P22-T04 | 定義 read-only observation reconciliation、metrics、evidence 與 P23 handoff contract | 不建立 production dashboard/API；驗收為可重放觀測與缺資料 fail-closed |

## 核心不變式與風險

- chain observation 不是 deposit credit；任何 observation 都不得改 balance、ledger、wallet accounting 或交易狀態。
- transaction identity 必須包含 chain/network、transaction hash、event/log index 或等價來源唯一鍵；不得只用本機時間。
- finality/reorg、duplicate、cursor gap、provider divergence 與 stale 必須可見且 fail-closed。
- 所有未來 runtime task 為 `HUMAN_REVIEW_REQUIRED`；provider license、rate limit、RPC/reconnect、secret handling 必須另外核准。

## 停止條件與下一步

任何 credit、地址派發、RPC 連線、migration 或 secret 需求出現即停止並建立個別 task card。P23 僅可在 P22 的依賴 task 全數完成後開始。

## P22-T01 實作紀錄

`com.lumix.deposit.address` 提供 `DepositNetwork`、`DepositAddress`、`DepositAddressOwnership` 與 pure ownership policy。EVM address 只以小寫 canonical identity 比較（未宣稱 checksum 驗證）；Base58/Bech32 只接受明確格式。相同 network/address 可供同一 owner 冪等重送，不同 owner 即使 asset 不同也會拒絕。沒有任何 wallet provisioning、地址生成、持久化、鏈上讀取或資產 credit。

## P22-T02 實作紀錄

`com.lumix.deposit.observation` 將觀測 identity 固定為 network、transaction ID 與 event/log index，並保存 block height/hash、原子整數數量與 provider finality snapshot。admission policy 使用 caller 提供的 immutable evidence 與 cursor 排序；相同內容 replay 才會被 idempotent 忽略，identity 相同但證據不同、network 不符、地址格式不符或游標倒退都會 fail-closed。沒有 RPC、provider secret、持久化、credit 或 ledger/balance mutation。

## P22-T03 實作紀錄

`com.lumix.deposit.observation.finality` 將同一 observation identity 的 successive finality snapshot 做 pure 比對：block hash 不同標記為 `ORPHANED` 與 reorg halt，confirmation 倒退標記為 `QUARANTINED` 與 regression halt；達最低 confirmation 僅表示 `FINALITY_THRESHOLD_MET`，絕非 credit。每個 network 有獨立且 sticky 的 health snapshot；cursor gap、stale provider signal 與 reorg 皆會 halt，唯有帶有 verified cursor/time 的明確 recovery evidence 可恢復。沒有 provider 連線、持久化、帳本、餘額或資產 mutation。

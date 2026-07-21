# Phase 22 - 入金地址與鏈上監聽器

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

僅有文件規劃；runtime、chain connection、secret、schema 與 credit 尚未開始，也沒有 implementation approval。

## 目標與依賴

建立 provider/chain-neutral 的入金地址所有權與鏈上觀測 foundation。施工必須先完成並通過 P21 review，並沿用 P13 identity/asset、P14 immutable ledger 的既有邊界；本 phase 不會 credit 資產。

## 詳細 task draft

| Draft | 目標與交付 | 禁止事項與驗收 |
| --- | --- | --- |
| P22-T01 | 定義 network、asset、address、ownership、address lifecycle 與唯一性契約 | 不產生地址、不寫 schema；測試 network/address normalization、重複 ownership、錯誤 asset |
| P22-T02 | 定義 provider-neutral chain observation、block/transaction/log identity、cursor 與 finality observation contract | 不連 RPC、不存 secret；測試 duplicate observation、錯鏈、重播與 deterministic ordering |
| P22-T03 | 定義 reorg、confirmation、orphan、halt/resume health state 與通知邊界 | 不 credit、不調 balance/ledger；測試 reorg、confirmation regression、gap/stale、multi-network 隔離 |
| P22-T04 | 定義 read-only observation reconciliation、metrics、evidence 與 P23 handoff contract | 不建立 production dashboard/API；驗收為可重放觀測與缺資料 fail-closed |

## 核心不變式與風險

- chain observation 不是 deposit credit；任何 observation 都不得改 balance、ledger、wallet accounting 或交易狀態。
- transaction identity 必須包含 chain/network、transaction hash、event/log index 或等價來源唯一鍵；不得只用本機時間。
- finality/reorg、duplicate、cursor gap、provider divergence 與 stale 必須可見且 fail-closed。
- 所有未來 runtime task 為 `HUMAN_REVIEW_REQUIRED`；provider license、rate limit、RPC/reconnect、secret handling 必須另外核准。

## 停止條件與下一步

任何 credit、地址派發、RPC 連線、migration 或 secret 需求出現即停止並建立個別 approved task card。P23 僅可在 P22 review 通過後開始其 runtime planning/implementation。

# Phase 24 - 提款請求流程

## 狀態

```text
COMPLETED_FOR_WITHDRAWAL_REQUEST_FOUNDATION
```

P24-T01 至 P24-T04 的 immutable request/eligibility/lifecycle/reconciliation contract 已完成；不簽章、不廣播、不釋放資金，也沒有提款 runtime approval。逐卡 approve 機制依人類指示暫停。

## 目標與依賴

建立 user request 到受控 approval queue 的 idempotent、可稽核提款請求邊界。P23 foundation 已完成；施工依賴 identity/auth、balance/reservation、ledger 與 audit 基礎；簽章與鏈上廣播只屬 P25。

## 詳細 task draft

| Draft | 目標與交付 | 禁止事項與驗收 |
| --- | --- | --- |
| P24-T01 | 定義 withdrawal request、destination、asset/network、idempotency key、request lifecycle 與 immutable audit event | COMPLETED；不簽章、不廣播；已測試重試、同 key 異 payload、錯 network/address、精度 |
| P24-T02 | 定義 eligibility、available balance、reservation/hold handoff、fee quote version 與 rejection reason | COMPLETED；不 capture/settle；已測試餘額不足、過期 quote、並行 request、risk rejection |
| P24-T03 | 定義 cancel、expire、manual-review queue 與 approval handoff 的狀態轉換 | COMPLETED；不授予 admin bypass；已測試 race、重複取消、過期、不可逆狀態 |
| P24-T04 | 定義 request/hold/audit/reconciliation evidence 與 P25 signer input contract | COMPLETED；不傳送私鑰或 signer command；已驗證可重放 state transition 與差異升級 |

## 核心不變式與風險

- request accepted 不代表 approved、signed、broadcast、settled 或資產已離開。
- destination、network、asset、amount、fee quote 與 idempotency key 必須綁定；不得用 float/double 或無版本 fee。
- hold/release/capture 必須可追蹤且可對帳；任何異常、風控不確定或 audit 缺失一律 fail-closed。
- 所有 runtime 卡均 `HUMAN_REVIEW_REQUIRED`，人類須審查權限、併發、資產保留與取消語意。

## 停止條件與下一步

簽章、HSM/MPC、broadcast、鏈上確認、提款完成與任何權限 bypass 都必須停止並交給 P25 的獨立批准 task card。

## P24-T01 實作紀錄

`com.lumix.withdrawal.request` 定義 request/user/asset/network/destination/atomic amount/idempotency key 與 request-created audit evidence。destination format 必須匹配 network；同 user/key 的完整 payload 重試才可重放，payload 不同即 fail-closed；atomic amount 上限由 caller 明確傳入。此層沒有 fee quote、餘額查詢、reservation/hold、approval、簽章、廣播或資產 mutation。

## P24-T02 實作紀錄

`com.lumix.withdrawal.request.eligibility` 對 caller 提供的 available balance evidence、asset/network/versioned fee quote、risk decision 與既有 pending handoff 做 pure 判定。amount+fee 與同 user/asset 的 pending handoff 都納入比較；餘額不足、過期或不符 quote、risk rejected/unknown 皆 fail-closed。eligible 結果僅產生未來 hold handoff envelope，不建立 hold、不 capture/settle、不查詢餘額或變更任何資產狀態。

## P24-T03 實作紀錄

`com.lumix.withdrawal.request.lifecycle` 將 request-created audit event 保留為每份 state 的第一筆不可變證據。transition 必須帶 expected lifecycle 作為 race 防線；cancel/expire/queue manual review/prepare approval handoff 均追加 audit event，而非覆寫歷史。重複 cancel 可冪等，stale state 與 approval handoff 後的不可逆操作都 fail-closed。approval handoff 僅是資料狀態，不是管理員 bypass、簽章或廣播。

## P24-T04 實作紀錄

`com.lumix.withdrawal.request.reconciliation` 只核對 immutable request state、hold evidence 與 audit trail。僅當 state 為 `APPROVAL_HANDOFF_READY`、有 matching hold evidence 且保留 approval-handoff audit event 時，才輸出含 SHA-256 audit digest 的 P25 input；沒有私鑰、簽名、transaction payload、signer command 或 broadcast。任一缺失都輸出 exception 並不提供 signer input。

# Phase 24 - 提款請求流程

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

僅有文件規劃；不簽章、不廣播、不釋放資金，也沒有提款 runtime approval。

## 目標與依賴

建立 user request 到受控 approval queue 的 idempotent、可稽核提款請求邊界。施工依序等待 P23 review，並依賴 identity/auth、balance/reservation、ledger 與 audit 基礎；簽章與鏈上廣播只屬 P25。

## 詳細 task draft

| Draft | 目標與交付 | 禁止事項與驗收 |
| --- | --- | --- |
| P24-T01 | 定義 withdrawal request、destination、asset/network、idempotency key、request lifecycle 與 immutable audit event | 不簽章、不廣播；測試重試、同 key 異 payload、錯 network/address、精度 |
| P24-T02 | 定義 eligibility、available balance、reservation/hold handoff、fee quote version 與 rejection reason | 不 capture/settle；測試餘額不足、過期 quote、並行 request、risk rejection |
| P24-T03 | 定義 cancel、expire、manual-review queue 與 approval handoff 的狀態轉換 | 不授予 admin bypass；測試 race、重複取消、過期、不可逆狀態 |
| P24-T04 | 定義 request/hold/audit/reconciliation evidence 與 P25 signer input contract | 不傳送私鑰或 signer command；驗收為可重放 state transition 與差異升級 |

## 核心不變式與風險

- request accepted 不代表 approved、signed、broadcast、settled 或資產已離開。
- destination、network、asset、amount、fee quote 與 idempotency key 必須綁定；不得用 float/double 或無版本 fee。
- hold/release/capture 必須可追蹤且可對帳；任何異常、風控不確定或 audit 缺失一律 fail-closed。
- 所有 runtime 卡均 `HUMAN_REVIEW_REQUIRED`，人類須審查權限、併發、資產保留與取消語意。

## 停止條件與下一步

簽章、HSM/MPC、broadcast、鏈上確認、提款完成與任何權限 bypass 都必須停止並交給 P25 的獨立批准 task card。

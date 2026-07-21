# Phase 28 - 稽核與合規證據匯出

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

建立可驗證、可重放、最小揭露的 audit/compliance evidence path；它提供證據與例外升級，不自動修復資金、交易或身份資料。

## 中階 task breakdown

1. Evidence model：資金、交易、風控、admin、security、chain observation 的 event completeness、correlation ID、retention 與 integrity metadata。
2. Read-only evidence projection：時間範圍、as-of、source/version、缺口狀態、replay digest 與可查詢性。
3. Export boundary：授權、資料最小化、遮罩、格式/版本、hash/manifest、可重現輸出與下載/交付稽核。
4. Compliance workflow boundary：case、hold、review、exception escalation、legal retention；不得設計 KYC/AML bypass 或自動資產處分。
5. Reconciliation/audit review：ledger/balance/reservation、deposit/withdrawal、sign/broadcast、risk/admin policy evidence 的一致性與不可修復 mismatch 報告。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。資料保留、PII、匯出授權、不可變證據與缺口表達必須人審；任何 evidence mismatch 只可升級調查，不得直接寫入修正。

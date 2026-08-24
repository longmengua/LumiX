# Phase 28 - 稽核與合規證據匯出

## 狀態

```text
COMPLETED_FOR_AUDIT_COMPLIANCE_EVIDENCE_FOUNDATION
```

## Phase charter

建立可驗證、可重放、最小揭露的 audit/compliance evidence foundation；它提供證據與例外升級，不自動修復資金、交易或身份資料。

## 中階 task breakdown

1. Evidence model：`COMPLETED_FOR_CONTRACT`；completeness、correlation、source/version 與 integrity digest。
2. Read-only evidence projection：`COMPLETED_FOR_CONTRACT`；gap/unknown fail-closed projection gate。
3. Export boundary：`COMPLETED_FOR_CONTRACT`；最小揭露、redaction profile、manifest evidence。
4. Compliance workflow boundary：`COMPLETED_FOR_CONTRACT`；case/escalation evidence，無處分。
5. Reconciliation/audit review：`COMPLETED_FOR_FOUNDATION`；缺口不可修復、只能調查升級。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。資料保留、PII、匯出授權、不可變證據與缺口表達必須人審；任何 evidence mismatch 只可升級調查，不得直接寫入修正。

# Phase 26 - 風控與限制

## 狀態

```text
COMPLETED_FOR_RISK_CONTROL_POLICY_FOUNDATION
```

## Phase charter

建立可版本化、可解釋、fail-closed 的風控決策與市場保護 foundation；不把 P19 sandbox risk 視為正式風控 runtime，也不授權任何 bypass、freeze 或 limit mutation。

## 中階 task breakdown

1. Policy model：`COMPLETED`；版本、effective time、atomic limit 與市場狀態契約。
2. Trusted inputs：`COMPLETED`；fresh/stale/gap/unknown input 均為 explicit evidence。
3. Deterministic evaluation：`COMPLETED`；replayable decision，不含 consumption/release。
4. Market/user protections：`COMPLETED_FOR_CONTRACT`；freeze/velocity/cooling-off/escalation signal 只轉為拒絕結果。
5. Governance/evidence：`COMPLETED_FOR_CONTRACT`；policy change dual-control evidence，不含 override 或持久化。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。任何 policy、override、freeze/unfreeze、limit consumption 或與 order/withdrawal 交互的 runtime 必須逐卡審核；不完整行情、資料衝突或無 audit 一律不可允許資金或交易動作。

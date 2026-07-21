# Phase 26 - 風控與限制

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

建立可版本化、可解釋、fail-closed 的風控決策與市場保護能力；不把 P19 sandbox risk 視為正式風控 runtime，也不授權任何 bypass。

## 中階 task breakdown

1. Policy model：帳戶、資產、商品、地區、時間窗、額度與市場狀態規則，以及 policy version/effective time。
2. Trusted inputs：P21 health/price、identity、balance/reservation、withdrawal request、鏈上狀態的讀取契約與 freshness；stale/gap/unknown input 一律降級或拒絕。
3. Deterministic evaluation：可重放 decision、優先序、reject reason、limit consumption/release boundary；不得由 wall clock 或 mutable cache 改變結論。
4. Market/user protections：circuit breaker、account freeze、velocity/abuse、withdrawal cooling-off 與人工 escalation；每項 action 明確區分 read、request、approved command。
5. Governance/evidence：policy change dual-control、override 最小權限、audit/reconciliation、regression/edge-case test matrix。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。任何 policy、override、freeze/unfreeze、limit consumption 或與 order/withdrawal 交互的 runtime 必須逐卡審核；不完整行情、資料衝突或無 audit 一律不可允許資金或交易動作。

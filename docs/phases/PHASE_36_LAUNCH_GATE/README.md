# Phase 36 - 正式上線門檻

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

彙整而非取代各 phase 的證據，依 `docs/governance/PRODUCTION_READINESS_GATES.md` 做正式上線判定。只有全數門檻通過並取得人類明確簽核，才可宣稱 production-ready。

## 高層任務

1. Evidence inventory：data integrity、funds safety、trading safety、security、operations、business launch 的可追溯證據與 owner。
2. Open-risk register：未解事項、risk acceptance、expiry、mitigation、launch blocker 與不可接受例外。
3. Go/no-go review：獨立人類審查、職責分離、sign-off、scope/region/product 限制與決議紀錄。
4. Controlled launch/rollback：phased exposure、kill switch、customer communication、incident escalation、success metric 與 abort criteria。
5. Post-launch gates：監控期間、reconciliation、security review、customer support、後續 expansion 必須再獲批准。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`。本 README 與任何 P36 文件都不是簽核；沒有所有 readiness gates、完整 evidence 與明確 human sign-off，狀態永遠不是 `PRODUCTION_READY`。

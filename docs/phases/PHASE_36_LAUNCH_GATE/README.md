# Phase 36 - 正式上線門檻

## 狀態

```text
COMPLETED_FOR_READINESS_DOCUMENTATION_NOT_PRODUCTION_READY
```

## Phase charter

建立彙整而非取代各 phase 證據的 launch-gate foundation，依 `docs/governance/PRODUCTION_READINESS_GATES.md` 做正式上線判定。foundation 與就緒文件組已完成；文件明確盤點缺口、證據來源、風險、no-go 判定及未來受控上線／回滾所需條件。只有全數門檻通過並取得人類明確簽核，才可宣稱 production-ready；目前沒有這些證據或簽核。

## 高層任務

1. Evidence inventory：`DOCUMENTED_GAP_BASELINE_COMPLETED`；`p36-t01-readiness-evidence-register.md` 已列出六類 gate 的最低證據、可接受驗證與目前缺口。這不是任何 gate 的通過證明。
2. Open-risk register：`DOCUMENTED_BLOCKER_REGISTER_COMPLETED`；`p36-t02-open-risk-register.md` 將所有當前缺口設為不可接受的 launch blocker。
3. Go/no-go review：`DOCUMENTED_NO_GO_DECISION_COMPLETED`；`p36-t03-go-no-go-decision.md` 記錄目前唯一有效判定為 `NO_GO`，人類簽核缺失一律不就緒。
4. Controlled launch/rollback：`DOCUMENTED_REQUIREMENTS_ONLY_NOT_IMPLEMENTED`；`p36-t04-controlled-launch-and-rollback.md` 定義未來必備 evidence 與 stop 條件，沒有 launch、rollback 或 kill-switch runtime。
5. Post-launch gates：`DOCUMENTED_REQUIREMENTS_ONLY_NOT_IMPLEMENTED`；`p36-t05-post-launch-gates.md` 定義未來上線後需重新取得的證據，不能視為已核准。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`。本 README 與任何 P36 文件都不是簽核；沒有所有 readiness gates、完整 evidence 與明確 human sign-off，狀態永遠不是 `PRODUCTION_READY`。

## 文件交付狀態

```text
P36-T01：就緒證據登錄與缺口基線 — DOCUMENTED_GAP_BASELINE_COMPLETED
P36-T02：開放風險登錄 — DOCUMENTED_BLOCKER_REGISTER_COMPLETED
P36-T03：Go/no-go 決策紀錄 — DOCUMENTED_NO_GO_DECISION_COMPLETED
P36-T04：受控上線與回滾要求 — DOCUMENTED_REQUIREMENTS_ONLY_NOT_IMPLEMENTED
P36-T05：上線後門檻要求 — DOCUMENTED_REQUIREMENTS_ONLY_NOT_IMPLEMENTED

文件層已完成；下一步只能由具備相應系統、資安、營運與商業權限的責任人，產出、驗證並簽核 evidence。未取得 evidence 的項目一律維持 NOT_READY。
```

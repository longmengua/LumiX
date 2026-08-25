# Phase 36 - 正式上線門檻

## 狀態

```text
READINESS_EVIDENCE_GAP_CLOSURE_DOCUMENTATION_IN_PROGRESS_NOT_PRODUCTION_READY
```

## Phase charter

建立彙整而非取代各 phase 證據的 launch-gate foundation，依 `docs/governance/PRODUCTION_READINESS_GATES.md` 做正式上線判定。foundation 已完成；目前開始以文件盤點缺口、證據來源與責任邊界。只有全數門檻通過並取得人類明確簽核，才可宣稱 production-ready；目前沒有這些證據或簽核。

## 高層任務

1. Evidence inventory：`DOCUMENTED_GAP_BASELINE_COMPLETED`；`p36-t01-readiness-evidence-register.md` 已列出六類 gate 的最低證據、可接受驗證與目前缺口。這不是任何 gate 的通過證明。
2. Open-risk register：`COMPLETED_FOR_BLOCKER_GATE`；缺 evidence 不能通過。
3. Go/no-go review：`COMPLETED_FOR_HUMAN_SIGN_OFF_GATE`；人類簽核缺失一律不就緒。
4. Controlled launch/rollback：`NOT_IMPLEMENTED`；不得建立 launch/kill switch runtime。
5. Post-launch gates：`NOT_IMPLEMENTED`；必須於正式 launch 後另行取得人類核准。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`。本 README 與任何 P36 文件都不是簽核；沒有所有 readiness gates、完整 evidence 與明確 human sign-off，狀態永遠不是 `PRODUCTION_READY`。

## 目前施工項目

```text
P36-T01：就緒證據登錄與缺口基線
狀態：DOCUMENTED_GAP_BASELINE_COMPLETED
範圍：只建立可稽核的證據需求、缺口與驗證責任邊界；不建立 runtime、不執行 production 驗證，也不改變任何 gate 結論。
下一步：由具備相應系統、資安、營運與商業權限的責任人，依登錄表產出可驗證 evidence；未取得 evidence 的項目一律維持 NOT_READY。
```

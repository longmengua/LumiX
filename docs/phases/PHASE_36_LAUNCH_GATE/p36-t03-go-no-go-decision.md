# P36-T03 Go/No-Go 決策紀錄

```text
Task: P36-T03
Status: DOCUMENTED_NO_GO_DECISION_COMPLETED
HUMAN_REVIEW_REQUIRED: yes
Current decision: NO_GO
Decision authority: human only
```

## 目前判定

目前唯一有效的 P36 判定是 `NO_GO`。P36-R01 至 P36-R06 均為 `OPEN_LAUNCH_BLOCKER`，六類權威 gate 也均為 `NOT_READY_EVIDENCE_MISSING`。沒有任何人類 production sign-off，因此沒有可用的 launch 授權、例外授權或條件式核准。

此紀錄是當前事實狀態，不是等待簽名的空白核准單。未來只有在所有下列入場條件已被獨立驗證時，具權限的人類才可建立新的、不可回填的決策紀錄。

## 未來 Go 決策的不可省略入場條件

1. P36-T01 的六個 gate 均有完整、可重現且未失效的 evidence reference。
2. P36-T02 的每個風險均有 append-only resolution record；不得遺漏或靜默接受未關閉 blocker。
3. 證據的系統版本、環境、資料範圍與驗證結果均已由獨立角色複核。
4. 受控上線／回滾能力已在不影響客戶資金的授權環境演練，並符合 P36-T04。
5. 上線後觀測、on-call、客戶支援與事件升級條件已依 P36-T05 確認。
6. 指定人類決策者以姓名、角色、日期、候選版本、環境、決策範圍與有效期限，明確記錄 `GO`；缺任一欄位即為 `NO_GO`。

任何人在一個領域自行產出的 evidence，不得自行審核並簽署同一領域的最終通過。資金、安全、合規與商業領域的人類決策權責必須由組織明確指定，不能由本文件或工程 agent 推定。

## 決策失效條件

即使未來產生 `GO`，下列任一情況都會使其立即失效並回到 `NO_GO`：候選版本、部署環境或風險範圍改變；證據過期或無法重現；新增 critical incident；任何 blocker 重開；或需要的角色簽核撤回／到期。

## 驗證與 rollback

文件驗證為決策明確是 `NO_GO`，並且 Go 的必要條件不能被任一單一文件或單一角色略過。本 task 僅建立決策邊界；revert 文件 commit 即可回復，沒有 runtime 或資金狀態變更。

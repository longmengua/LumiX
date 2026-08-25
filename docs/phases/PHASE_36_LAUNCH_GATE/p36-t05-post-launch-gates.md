# P36-T05 上線後門檻要求

```text
Task: P36-T05
Status: DOCUMENTED_REQUIREMENTS_ONLY_NOT_IMPLEMENTED
HUMAN_REVIEW_REQUIRED: yes
Runtime status: NOT_IMPLEMENTED
```

## 目的與邊界

正式上線即使在未來獲得人類 `GO`，也不是永久通過。此文件定義需要重新蒐集與審查的上線後 evidence。現階段沒有 production exposure，因此不存在任何上線後資料、監控結果或有效 post-launch 決策。

## 未來必須重新驗證的 gate

| 時點 | 最低審查內容 | 失敗時的結論 |
| --- | --- | --- |
| 首次 customer exposure 前 | P36-T03 的 `GO` 尚有效、所有 blocker 維持關閉、監控與事件回應責任人可聯繫。 | `NO_GO`，不得擴大 exposure。 |
| 每次版本、設定或依賴變更前 | 變更是否使既有 evidence 失效，並重新檢查安全、資金、交易與營運影響。 | 證據失效即停止變更或回到 P36 evidence collection。 |
| 每次資金／交易／安全事件後 | 影響範圍、帳本／餘額／交易完整性、客戶保護與事件處置紀錄。 | 視為 blocker 重開；禁止以舊 `GO` 延續。 |
| 定期營運審查 | 對帳、備份還原、告警、on-call、合規與客服流程的實際演練證據。 | 缺少任一必要 evidence 時維持或恢復 `NOT_READY`。 |

## 不變式

- 上線後 evidence 不能覆寫或刪除 P36-T01 的上線前基線與失敗紀錄。
- 指標、告警或客服回報不足以單獨證明資金與交易安全；每個 gate 都要維持各自的 evidence。
- 任何不確定、遺失或互相矛盾的 evidence 都必須以 fail-closed 處理，並交由人類決策。

## 驗證與 rollback

文件驗證為未來 gate、重開條件與 fail-closed 結論皆已定義，且沒有假稱存在上線後 runtime 或 evidence。本 task 僅改文件；revert 文件 commit 即可。

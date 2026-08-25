# P36-T04 受控上線與回滾要求

```text
Task: P36-T04
Status: DOCUMENTED_REQUIREMENTS_ONLY_NOT_IMPLEMENTED
HUMAN_REVIEW_REQUIRED: yes
Runtime status: NOT_IMPLEMENTED
```

## 目的與邊界

本文件定義未來受控上線與回滾必須證明的安全條件，避免把 P36 的文件完成誤當成可執行 launch plan。目前沒有 deployment workflow、feature flag、kill switch、資金停用器、rollback automation 或客戶 exposure；本文件不能直接用於 production 操作。

## 未來受控上線的最低要求

- 唯一且可追溯的候選版本、部署產物、設定版本與環境核對結果。
- 在 deployment 前重新確認 P36-T03 的有效 `GO`；任何狀態不是 `GO` 時，部署操作必須 fail-closed。
- 明確定義可觀測的成功條件、每項指標的資料來源、監看人員與最長觀察窗口。
- 與資金、交易、安全、營運責任人一致的 customer exposure 範圍；不得由工程便利性自行擴大。
- 已驗證的停止／回滾決策權責、通訊鏈路、資料完整性檢查與客戶溝通責任。

## 必須停止或回滾的類別

下列任何一類事件在未來都必須使 launch 停止或升級為人類回滾決策，不得自動忽略：資金對帳不一致、帳本或餘額不變式失敗、交易最終性或費用錯誤、未授權存取／secret 外洩跡象、觀測失明、備份／復原失敗、或合規／客服關鍵流程失效。

具體門檻值、回滾機制、資料補償與客戶溝通內容屬後續經人類核准的 runtime／營運設計，不能在沒有相應實作與授權時假定存在。

## 驗證與 rollback

文件驗證為所有要求都明示「未實作」，且沒有任何可執行的 deployment、kill-switch 或資金操作指令。本 task 無 runtime；若需撤回，revert 文件 commit 即可。

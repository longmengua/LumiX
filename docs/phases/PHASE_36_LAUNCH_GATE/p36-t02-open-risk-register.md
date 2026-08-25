# P36-T02 正式上線開放風險登錄

```text
Task: P36-T02
Status: DOCUMENTED_BLOCKER_REGISTER_COMPLETED
HUMAN_REVIEW_REQUIRED: yes
Scope: 只登錄風險與 closure evidence；不接受風險、不變更系統，也不授權 launch。
```

## 登錄規則

此登錄表是 P36-T01 的 fail-closed 延伸。只有能對應到 evidence 識別碼、獨立驗證結果與適當人類決策的風險，才可標示 `CLOSED`。所有列出的風險目前都沒有這些 evidence，故均為 `OPEN_LAUNCH_BLOCKER`。

工程 agent、單一領域實作者或文件作者都不得關閉此表的風險。資金安全與交易安全風險不得以風險接受取代 runtime 與驗證；任何例外若缺具名、具期限的人類接受紀錄，仍是 blocker。

| 風險 ID | 領域 | 現況與影響 | 狀態 | 關閉所需 evidence | 取證／核可責任 |
| --- | --- | --- | --- | --- | --- |
| P36-R01 | 資料完整性 | 尚無候選版本的 migration 重跑、rollback 與 schema inspection 演練，資料變更可能不可驗證或不可回復。 | `OPEN_LAUNCH_BLOCKER` | 受控 PostgreSQL 環境的版本化 migration、rollback、精度、唯一鍵、idempotency、outbox/audit 與 append-only 查核結果。 | 資料平台、後端與獨立審查者。 |
| P36-R02 | 資金安全 | ledger、餘額、reservation、入金 credit 與提款完成 runtime 未落地，不能證明資金正確性或避免重複支出。 | `OPEN_LAUNCH_BLOCKER` | 端到端不變式、重建、對帳、異常／reorg、approval/sign/broadcast/reconciliation 演練；所有證據需對應候選版本。 | 資金系統負責人與獨立風險／安全審查者。 |
| P36-R03 | 交易安全 | matching execution、settlement、正式風控與 fee runtime 未落地，不能驗證交易最終性、補償與市場保護。 | `OPEN_LAUNCH_BLOCKER` | deterministic matching、atomicity／補償、fee rounding、停市與風控情境的整合測試及操作演練。 | 交易、風控與獨立審查者。 |
| P36-R04 | 安全 | authN/authZ、secret lifecycle、transport rate/abuse enforcement 仍無 production runtime 與獨立測試。 | `OPEN_LAUNCH_BLOCKER` | 滲透／控制測試範圍與結果、權限審查、secret handling 查核、rate/abuse 驗證與提款職責分離證據。 | 資安責任人；不得由功能開發者自行核可。 |
| P36-R05 | 營運 | telemetry、alert、backup/restore/DR、on-call 與交接尚無可執行驗證。 | `OPEN_LAUNCH_BLOCKER` | 觀測訊號、告警演練、runbook 執行紀錄、restore/replay 演練與人員交接紀錄。 | 營運責任人與值班管理者。 |
| P36-R06 | 商業上線 | fee/revenue、條款、合規、客服與最終人類簽核尚未具備已核可紀錄。 | `OPEN_LAUNCH_BLOCKER` | 核可的商業／法務／合規／客服作業資料，以及具姓名、角色、日期與決策範圍的 sign-off。 | 商業、法務、合規與指定上線決策者。 |

## 更新與審計

風險處理應以 append-only 的 resolution record 加在本文件之後；必須保留原始 `OPEN_LAUNCH_BLOCKER`、evidence reference、驗證者、決策者、時間與失效條件。不得覆寫初始狀態、刪除失敗紀錄，或以任一其他 gate 通過推定本風險已關閉。

## 驗證與 rollback

文件驗證為六個 P36-T01 gate 各有唯一 blocker，且沒有任何風險被錯誤標為 `CLOSED`。本 task 無程式、schema、資料或資金變更；若需撤回，revert 文件 commit 即可。

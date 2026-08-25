# P36-T01 正式上線就緒證據登錄與缺口基線

```text
Task: P36-T01
Status: DOCUMENTED_GAP_BASELINE_COMPLETED
HUMAN_REVIEW_REQUIRED: yes
Scope: 文件與證據登錄；不含 production runtime、資料修復、資金移動或 launch 決策。
Authoritative gate: docs/governance/PRODUCTION_READINESS_GATES.md
```

## 任務目的

將權威正式上線門檻拆成可稽核的最低證據要求，明確記錄目前 repository 已知能力與尚未證明的缺口。此登錄表的目的，是避免把 phase foundation、文件、單元測試或設計契約誤認為正式營運證據。

任何一項未附上可驗證、可追溯且由適當責任人確認的 evidence，狀態必須是 `NOT_READY_EVIDENCE_MISSING`。本文件不授權執行 production 操作，也不能取代人類 go/no-go 簽核。

## 證據接受規則

每項 evidence 至少必須具備下列資訊，才能交由 P36 go/no-go review 使用：

- 證據識別碼與產出時間；時間必須足以判斷是否仍反映候選上線版本。
- 對應 gate、系統版本或 commit、環境、資料範圍與執行者／責任人。
- 可重現的驗證程序、實際結果與失敗／跳過項目；單有「已測」敘述不構成 evidence。
- 來源保存位置與完整性資訊，例如受控文件版本、簽章、checksum 或不可竄改 audit reference。
- 已知限制、例外、風險接受者與到期日；沒有明確接受者的例外必須阻擋 launch。

下列來源單獨不足以使 gate 通過：程式介面、mock、純 in-memory 測試、文件宣告、過期測試結果、未簽核 screenshot，或沒有版本／環境資訊的口頭確認。

## 門檻登錄

| Gate | 最低可接受 evidence | 目前已知狀態 | 阻擋原因與下一個取證責任 |
| --- | --- | --- | --- |
| 資料完整性 | production-like PostgreSQL migration 重跑與 rollback 演練紀錄；金額精度、唯一鍵、idempotency、outbox/audit、append-only schema 的 schema inspection 結果 | `NOT_READY_EVIDENCE_MISSING` | 現有 foundation 與測試不等同候選版本的可重跑演練；資料平台與後端責任人須在受控環境產出紀錄。 |
| 資金安全 | ledger 不變式、可重建餘額、hold/release/capture 對帳、入金確認、提款 approval/sign/broadcast/reconciliation 的整合證據與異常演練紀錄 | `NOT_READY_EVIDENCE_MISSING` | 正式 ledger、餘額、入金 credit、提款 signer/broadcast runtime 均未完成；此項不得以 foundation 補件或風險接受取代。 |
| 交易安全 | 訂單 lifecycle、deterministic matching、結算 atomicity／補償、fee rounding、停市與風控流程的整合測試與操作演練 | `NOT_READY_EVIDENCE_MISSING` | matching execution、settlement 與正式風控 runtime 尚未開始；交易與風控責任人不得簽為通過。 |
| 安全 | authN/authZ 測試、admin audit trail、secret handling 查核、rate/abuse 防護壓測、提款權限分離的獨立安全審查結果 | `NOT_READY_EVIDENCE_MISSING` | P27、P29、P33 僅提供 contract foundation，沒有 authentication/authorization、secret 或 transport enforcement runtime 證據；資安責任人須獨立驗證。 |
| 營運 | metrics/logs/traces/alerts 觀測、runbook 演練、incident escalation、backup/restore/replay 演練、on-call handover 的時間戳記紀錄 | `NOT_READY_EVIDENCE_MISSING` | P31、P32、P35 為 evidence contract；沒有 telemetry、alert、restore/DR 或人員演練 runtime。營運責任人須產出受控演練紀錄。 |
| 商業上線 | 可管理 fee schedule、revenue ledger 查詢、核可的 terms/compliance/support 流程，以及具姓名、角色、日期與決策範圍的 human sign-off | `NOT_READY_EVIDENCE_MISSING` | 商業、法務、合規與支援流程尚無人類確認；工程團隊無權代替簽核。商業與合規責任人須先提供正式決策資料。 |

## 依賴與判定順序

```text
各領域 runtime 與受控環境驗證
              |
              v
P36-T01 evidence 登錄更新與完整性檢查
              |
              v
未解 blocker? -- 是 --> NOT_READY，禁止 launch
              |
              否
              v
獨立人類 go/no-go 簽核
              |
              v
才可依權威門檻宣稱 PRODUCTION_READY
```

P36 只彙整證據，不能透過文件變更、狀態字串或工程 agent 自行核准來略過任何領域 gate。資金安全與交易安全的 runtime 缺口是上游阻擋項；在其完成並有獨立 evidence 前，後續安全、營運或商業文件完成也不會解除 launch 禁令。

## 驗證與維護

本 task 的文件驗證為：

1. 每一項 `PRODUCTION_READINESS_GATES.md` 中的 gate 均有一列，且列出最低 evidence、目前狀態與責任邊界。
2. 每一列的目前狀態均為 `NOT_READY_EVIDENCE_MISSING`；沒有任何未驗證項目被寫成通過。
3. P36 README 與 `AI_PROGRESS.md` 均指向本 task，且持續明示 production-ready 與 launch 禁止。

將來新增 evidence 時，必須保留本基線並以 append-only 方式新增 evidence reference、驗證結果、審查者與失效條件；不得覆寫缺口歷史或以沒有 evidence 的狀態直接改為 `READY`。

## 明確未完成事項

- 沒有收集、驗證或簽核任何正式上線 evidence。
- 沒有實作 production ledger、餘額、hold、入金、提款、matching、settlement、auth、監控、備份還原或商業營運 runtime。
- 沒有 controlled launch、rollback、kill switch、對客溝通或 post-launch gate。
- 沒有也不能產生 human production sign-off。

## Rollback

本 task 僅新增／更新文件；若需撤回，revert 此 task 的文件 commit 即可，沒有 schema、資料或資金狀態需要回復。

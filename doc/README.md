# doc

## 文件分類（目前結構）

- `doc/overview.md`：專案總覽、產品與本機啟動入口
- `doc/status`：目前狀態與 production blocker
- `doc/roadmap`：TODO、release checklist、任務索引與規劃筆記
- `doc/architecture`：架構、資料結構、持久化、Kafka 與 Redis 相關設計
- `doc/operations`：觀測、告警、SMTP、監控導向文件
- `doc/validation`：交易所功能驗收、demo 驗證與回歸檢查矩陣
- `doc/reliability`：風險控制、運維 runbook、可用性與操作流程
- `doc/runbooks`：災備、發布節點、跨系統重放與停機演練
- `doc/integrations`：外部整合、Polymarket
- `doc/ai`：Codex / Agent 協作文件（保留原結構）
- `doc/tasks`：任務中心（含 active 與 backlog；若目錄存在時使用）

## 直接入口

- [專案總覽：`doc/overview.md`](./overview.md)
- [交易所功能驗證表：`doc/validation/exchange-function-validation.md`](./validation/exchange-function-validation.md)
- [任務索引：`doc/roadmap/task-index.md`](./roadmap/task-index.md)

## 建議閱讀順序

1. 先讀 `doc/overview.md`
2. 再讀 `doc/status/current-state.md`、`doc/roadmap/todo.md`
3. 需要實作則對照 `doc/roadmap/task-index.md` 或現有 `doc/tasks/README.md`
4. 查資料時以各子目錄的索引檔為主

# docs

這份文件改為「主題目錄」版，不再以 `en/`、`zh-TW/` 當第一層分類。

## 文件分類（新結構）

- `docs/project`：專案總覽、目前狀態、TODO、技術總覽
- `docs/architecture`：架構、資料結構、持久化、Kafka 與 Redis 相關設計
- `docs/operations`：觀測、告警、SMTP、監控導向文件
- `docs/reliability`：風險控制、運維 runbook、可用性與操作流程
- `docs/runbooks`：災備、發布節點、跨系統重放與停機演練
- `docs/integrations`：外部整合、Polymarket、訊息中心 API
- `docs/ai`：Codex / Agent 協作文件（保留原結構）
- `docs/tasks`：任務中心（含 active 與 backlog）

## 直接入口

- [DOCUMENTATION-INDEX.md](./DOCUMENTATION-INDEX.md)
- [專案總覽：`docs/project/README.md`](./project/README.md)
- [訊息中心任務規格：`docs/tasks/2026-06-16-0620-message-center-task.md`](./tasks/2026-06-16-0620-message-center-task.md)
- [任務入口：`docs/tasks/README.md`](./tasks/README.md)

## 建議閱讀順序

1. 先讀 `docs/project/README.md`
2. 再讀 `docs/project/current-state.md`、`docs/project/todo.md`
3. 需要實作則對照 `docs/tasks/README.md`、`docs/tasks/active.md`
4. 查資料時以 `docs/DOCUMENTATION-INDEX.md` 的主題索引為主

# LumiX

`LumiX` 是交易所專案代號，名稱來自 `Lumi` 與 `X`。

`Lumi` 帶有光、照亮與清晰可見的意象，`X` 代表 `exchange`。

LumiX 的目標是建立一套可正式營運的線上交易所系統，涵蓋使用者介面、後端服務、交易核心、錢包、風控、對帳與營運支援。

## 建議閱讀入口

- 第一次看專案：`docs/README.md`
- 後台 UI 設計：[LumiX Institutional Blue](docs/governance/lumix-institutional-blue.md)
- 先理解專案範圍：`docs/reference/overview.md`
- 先理解權威狀態與施工節奏：`docs/governance/OPERATING_EXCHANGE_MASTER_PLAN.md`
- 先理解整體系統形狀：`docs/architecture/ARCHITECTURE_TEXT_MAP.md`
- AI / agent 開工：`AGENTS.md`、`AI_AGENT.md`、`AI_PROGRESS.md`、`docs/ai/AI_CONTEXT_ROUTING.md`

## 目錄概覽

- `web/`：React + TypeScript + Vite 前端
- `server/`：Java 21 + Spring Boot 3 後端
- `docs/`：治理、規劃、架構、產品、phase 與 AI 協作文件

## 本機開發執行方式

Docker Compose 僅啟動有狀態依賴：PostgreSQL、Redis、Kafka。Spring Boot API、前台 Vite 與後台 Vite 必須以本機程序各自啟動，分別使用 `8080`、`8088`、`8089`；後台 Vite 的 HMR 會直接反映原始碼變更。完整的環境變數與啟動順序見 `docs/operations/compose-runtime-foundation.md`。

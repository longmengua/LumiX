# LumiX

LumiX 是交易所專案，包含前台、後台、API、交易核心、錢包、風控、對帳與營運支援。

- `web/` 為 React、TypeScript、Vite 前端。
- `server/` 為 Java 21、Spring Boot 3 後端。
- `docs/` 為產品、架構、治理、工程與營運規格。

AI 開發入口為 `AGENTS.md`、`AI_AGENT.md` 與 `docs/ai/AI_CONTEXT_ROUTING.md`。文件導覽見 `docs/README.md`。

本機 Docker Compose 啟動 PostgreSQL、Redis、Kafka。API、前台、後台以主機程序分別使用可配置埠（預設 8080、8090、8100）；Nginx 預設在標準 HTTPS 443 提供入口。啟動細節見 `docs/operations/compose-runtime-foundation.md`。

<!-- File purpose: Repository documentation catalog grouped by product docs, technical docs, and TODO docs. -->
# Java21 Match Hub Documentation

Java21 Match Hub is a Java 21 + Spring Boot backend for an exchange core and Polymarket integration.

## Documentation Categories

| Category | English Description | 中文說明 | English | 繁體中文 |
| --- | --- | --- | --- | --- |
| Product Documentation | Business-facing overview: product scope, business features, business modules, APIs, and the order placement journey. | 面向產品與業務的總覽：產品範圍、業務功能、業務模塊、API 與下單鏈路。 | [docs/en/README.md](docs/en/README.md) | [docs/zh-TW/README.md](docs/zh-TW/README.md) |
| Current State | Quick status dashboard: completion level, MVP baseline, production blockers, and near-term priorities. | 目前狀態儀表板：完成度、MVP baseline、production blocker 與近期優先順序。 | [docs/en/current-state.md](docs/en/current-state.md) | [docs/zh-TW/current-state.md](docs/zh-TW/current-state.md) |
| Technical Documentation | Engineering-facing notes: architecture review, module structure, API curl scripts, matching engine notes, local infrastructure, and implementation details. | 面向工程的說明：架構分析、模塊結構、API curl 腳本、撮合引擎說明、本機基礎設施與實作細節。 | [docs/en/technical.md](docs/en/technical.md) | [docs/zh-TW/technical.md](docs/zh-TW/technical.md) |
| TODO Documentation | Production-readiness roadmap: required work before real funds or production traffic, grouped by priority and domain. | 正式環境待辦清單：真實資金或正式流量前需要補齊的工作，依優先級與領域分類。 | [docs/en/todo.md](docs/en/todo.md) | [docs/zh-TW/todo.md](docs/zh-TW/todo.md) |

## Local Startup

```bash
docker compose up -d
./mvnw spring-boot:run
```

The default API endpoint is `http://localhost:8080`.

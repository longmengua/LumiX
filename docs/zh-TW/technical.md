<!-- 檔案用途：繁體中文技術文件索引；英文版本位於 ../en/technical.md。 -->
# 技術文件

技術文件面向工程人員，說明系統結構、操作方式與後續擴充方向。

English version: [../en/technical.md](../en/technical.md)

## 文件清單

| 文件 | 說明 |
| --- | --- |
| [目前狀態](current-state.md) | 快速判斷目前是 MVP baseline，並列出已完成能力、production blocker 與近期優先順序。 |
| [技術筆記](NOTES_ch.md) | 架構與實作審查，涵蓋分層、已完成能力、風險與 production 優先事項。 |
| [AI Code Map](../ai/code-map.md) | 給 Codex/代理使用的精簡程式碼責任與流程地圖。 |
| [AI 文件](ai.md) | Agent maps 與任務入口流程的繁中索引。 |
| [任務文件](tasks.md) | 可點名 task md 的繁中索引。 |
| [API curl 腳本](../../shells/api-curls/README_ch.md) | 本機 exchange 與 Polymarket API curl 腳本使用說明。 |
| [撮合引擎](../../src/main/java/com/example/exchange/infra/matching/README_ch.md) | 目前 in-memory matching engine 說明與 production 演進方向。 |
| [Redis Key Schema](redis-key-schema.md) | Redis key 歸屬、TTL 規則、namespace policy 與 migration backlog。 |
| [Kafka Topics](kafka-topics.md) | Topic matrix、partition key policy、consumer groups、schema versioning 與正式環境建立規則。 |
| [Observability Baseline](observability.md) | Request/correlation ID 在 HTTP、OkHttp、Kafka outbox 與 audit logs 的傳遞基線。 |
| [Archive Strategy](archive-strategy.md) | 歷史資料的 archive manifest、retention class、刪除前置條件與 restore 規則。 |
| [Outbox Runbook](outbox-runbook.md) | Durable outbox、DLQ replay 與人工補償操作流程。 |
| [Matching Sequencer Runbook](matching-sequencer-runbook.md) | 單 symbol sequencer 的 production ownership、partition、啟動與 failover 規則。 |
| [產品總覽](README.md) | 業務功能、模塊、下單鏈路、API 與 Kafka topics。 |
| [待辦清單](todo.md) | Production readiness 的工程待辦路線圖。 |

## 範圍

- 系統架構與 package 邊界。
- 本機基礎設施：MySQL、Redis、Kafka、Kafka UI。
- API 測試腳本與手動驗證流程。
- 撮合引擎行為與目前限制。
- 目前完成度、MVP baseline 與 production blocker。
- 真實流量前需要補齊的 production 工程缺口。

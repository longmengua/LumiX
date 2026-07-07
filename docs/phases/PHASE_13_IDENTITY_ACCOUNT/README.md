# 第 13 階段 - Backend Module Foundation & API Boundary

## 狀態

```text
in progress
```

## 目標

建立 Spring Boot 後端的 module 邊界與 API 分層準則，讓後續 identity、account、ledger、wallet、order、trade 等 runtime 可以安全接入。

## 不在範圍內

```text
runtime money movement
ledger posting service
reservation hold / release
order matching
settlement execution
withdrawal signing / broadcast
deposit crediting
Flyway migration 變更
前端變更
```

## 必要閱讀

```text
AGENTS.md
AI_AGENT.md
AI_PROGRESS.md
docs/ai/AI_CONTEXT_ROUTING.md
server/docs/backend-module-boundary.md
```

## 建議模組邊界

```text
common
security
user
account
asset
market
wallet
ledger
reservation
order
trade
outbox
audit
admin
```

## 建議分層

```text
api
application
domain
persistence
```

## 任務順序

```text
P13-T01 backend package architecture and module boundary
P13-T02 common error response / exception boundary
P13-T03 request validation and DTO convention
P13-T04 repository / persistence access convention
P13-T05 transaction boundary policy
P13-T06 API versioning and OpenAPI documentation boundary
P13-T07 security boundary skeleton
P13-T08 integration test foundation
```

## 完成條件

- 有 backend module boundary 文件。
- 有 package skeleton 或 package marker。
- 不搬移大量既有程式碼。
- 不加入 runtime 邏輯。
- 不修改 migration。
- 有清楚的驗證方式。

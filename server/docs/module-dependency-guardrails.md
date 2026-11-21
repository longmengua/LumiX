# Module Dependency Guardrails

## 目的

本文件定義 LumiX 後端 module dependency guardrails。

## 依賴方向

```text
common -> all modules
api -> application, security, common
application -> domain, persistence port, security, common
persistence -> domain, common
security -> common, api.error
```

## 禁止方向

- `api` 不得直接依賴 `persistence implementation`
- `domain` 不得依賴 `api DTO`
- `repository` / `persistence` 不得回傳 `api DTO`
- `ledger`、`reservation`、`wallet`、`withdrawal`、`order`、`trade`、`settlement` 等高風險 module 不得互相直接亂 call
- 目前高風險 module 名單至少包含 `ledger`、`reservation`、`wallet`、`withdrawal`、`order`、`trade`、`settlement`

## 現階段可接受的例外

- `api.error` 可以映射 `persistence exception` 與 `security exception` 到安全的 `ApiErrorResponse`
- 這種例外只存在於 boundary adapter，不代表 persistence 或 security runtime 直接暴露給 API
- 目前 skeleton 只做 source-level guardrail，未來若引入更完整的架構測試工具，再加強 package 邊界掃描

## 文字圖

```text
+--------+     +-------------+     +-------------+
| common | --> | api         | --> | application |
+--------+     +-------------+     +-------------+
                         |                 |
                         v                 v
                    +----------+     +-----------+
                    | security |     | persistence
                    +----------+     +-----------+
```

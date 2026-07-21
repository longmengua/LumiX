# Phase 29 - 公開與私有 API 強化

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

將已批准的唯讀與 command domain contract 安全地暴露為版本化 API；不把 OpenAPI metadata、mock 或 sandbox route 當成可公開的 production service。

## 高層任務

1. API inventory、versioning、compatibility/deprecation 與明確 read/write contract。
2. Authentication、API key scope、session/nonce、authorization 與敏感 endpoint 職責分離。
3. Idempotency、concurrency、error taxonomy、precision serialization、time/health semantics。
4. Rate limit、abuse/DDoS protection、pagination/filtering bounds、request validation 與安全 logging。
5. Contract/integration/security test、consumer migration、rollback/version coexistence evidence。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；未完成 P26–P28 review、安全設計與每個 command 的個別核准前，不得對外啟用資金或交易 endpoint。

# Phase 29 - 公開與私有 API 強化

## 狀態

```text
COMPLETED_FOR_API_ADMISSION_CONTRACT_FOUNDATION
```

## Phase charter

建立已批准 domain contract 的版本化 API admission foundation；不把 OpenAPI metadata、mock 或 sandbox route 當成可公開的 production service。

## 高層任務

1. API inventory、versioning、compatibility/deprecation 與明確 read/write contract：`COMPLETED_FOR_CONTRACT`。
2. Authentication、API key scope、session/nonce、authorization 與敏感 endpoint 職責分離：`BOUNDARY_ONLY`，沒有 auth runtime。
3. Idempotency、concurrency、error taxonomy、precision serialization、time/health semantics：`COMPLETED_FOR_ADMISSION_CONTRACT`。
4. Rate limit、abuse/DDoS protection、pagination/filtering bounds、request validation 與安全 logging：`COMPLETED_FOR_RATE_GATE_CONTRACT`，沒有 transport enforcement。
5. Contract/integration/security test、consumer migration、rollback/version coexistence evidence：`COMPLETED_FOR_FOUNDATION`。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；未完成 P26–P28 review、安全設計與每個 command 的個別核准前，不得對外啟用資金或交易 endpoint。

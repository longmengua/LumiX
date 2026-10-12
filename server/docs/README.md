# 後端

Java backend 的責任是處理 API、交易邊界、資料一致性、流程協調與審計。

```text
module-map.md              modules and ownership
transaction-boundary.md    transaction rules
api-contract-guidelines.md API rules
error-policy.md            error model
idempotency-policy.md      duplicate request safety
backend-module-boundary.md module and layer boundaries
domain-boundaries.md       asset and account ownership boundaries
dto-conventions.md         API DTO naming and exposure rules
validation-conventions.md  API validation rules
error-response-boundary.md safe error-response contract
persistence-boundary.md    persistence access boundary
transaction-boundary-policy.md application transaction policy
api-versioning-openapi-boundary.md API version and OpenAPI boundary
security-boundary.md       security and principal boundary
module-dependency-guardrails.md package dependency guardrails
sepolia-withdrawal-e2e.md    Sepolia 提款受控手動 E2E 操作手冊
withdrawal-architecture.md   提款 runtime 架構與資金安全邊界
withdrawal-risk-engine.md    提款風控規則與 Redis velocity 設計
physical-signer.md           獨立 signer trust boundary 與安全限制
wallet-architecture-after-withdrawal.md 提款 runtime 後的後端錢包架構快照
server-service-overview.md  server service scope and document entry points
```

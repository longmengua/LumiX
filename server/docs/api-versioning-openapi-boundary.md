# API Versioning and OpenAPI Boundary

## 目的

本文件定義 LumiX 後端 API versioning 與 OpenAPI 文件邊界。
Phase 13 只先固定路徑與文件規範，不實作真正 controller，也不引入 Swagger runtime 依賴。

## API versioning 規則

- API path 必須明確版本化。
- 目前基準版本使用 `/api/v1`。
- 後續新增 endpoint 必須從版本前綴往下掛載，例如 `/api/v1/users`。
- 未版本化 path 不應成為正式對外契約。

## 建議 path 形式

```text
/api/v1/health
/api/v1/users
/api/v1/accounts
/api/v1/orders
```

## OpenAPI 文件邊界

- OpenAPI 文件不得洩漏 internal package 結構。
- OpenAPI 文件不得洩漏 stack trace、SQL、secret、private key 或 wallet signing 細節。
- error response schema 必須對齊 `ApiErrorResponse`。
- request / response schema 必須對齊 DTO convention。

## 高風險 API 標記

下列 API 類型在文件中必須標記 `HUMAN_REVIEW_REQUIRED` 或 `production-gated`：

```text
ledger
withdrawal
order
settlement
admin
risk
```

## 文字圖

```text
+------------------+     +------------------+     +---------------------+
| /api/v1/...      | --> | DTO convention   | --> | OpenAPI document    |
+------------------+     +------------------+     +---------------------+
```

## Phase 13 原則

- 先定義 versioned path，再讓後續 controller 採用。
- 不在此階段引入 Swagger UI 或 OpenAPI codegen runtime。
- 不公開 internal package 名稱作為對外契約。

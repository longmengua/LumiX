# DTO Conventions

## 目的

本文件定義 LumiX 後端 API request / response DTO 的命名與使用規則。
Phase 13 先把 DTO 邊界固定，避免後續各 bounded context 各自長出不同格式。

## 命名規則

```text
XxxRequest  - API request DTO
XxxResponse - API response DTO
XxxView     - read-only view DTO
```

## 使用規則

- `Request` DTO 只放 API 輸入，不直接使用 entity 或 persistence model。
- `Response` DTO 只放對外可公開欄位，不洩漏 internal id、secret、private key、SQL 或 stack trace。
- `View` DTO 是唯讀查詢模型，適合 query result 或 aggregate snapshot。
- 金額、價格、數量欄位一律使用 `BigDecimal`，不得使用 `float` 或 `double`。
- `requestId`、`clientOrderId`、`amount`、`price`、`quantity`、`address` 這些高風險欄位必須有繁體中文維護性註解。

## DTO boundary

```text
API Request -> validation -> application use case -> response/view
```

禁止事項：

- 直接把 JPA entity 當 request / response
- 直接把 database row model 當 API contract
- 在 response 中暴露內部欄位或敏感欄位

## 文字圖

```text
+----------------+     +------------------+     +------------------+
| XxxRequest     | --> | validation layer | --> | XxxResponse/View |
+----------------+     +------------------+     +------------------+
```

## Phase 13 原則

- 先固定命名與欄位語意，再讓後續 API 慢慢接入。
- 不把 DTO 當成 domain entity。

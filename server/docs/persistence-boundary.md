# Persistence Boundary

## 目的

本文件定義 LumiX 後端的 repository / persistence 存取邊界。
這一層只負責資料存取與 mapping，不負責交易 runtime，也不負責把 database client 直接暴露給 controller。

## 存取規則

- `api` / controller layer 不得直接呼叫 database client。
- `application` service 只能透過 repository / port 存取 persistence。
- repository 不應回傳 API DTO。
- repository 不應直接執行資金異動 runtime 規則。
- persistence exception 必須被轉換成安全的 domain / API error。

## 建議責任

```text
repository  -> data access, query, mapping
port        -> application 對 persistence 的抽象入口
mapper      -> domain / persistence model 轉換
```

## 高風險資料表

下列資料表相關存取要特別保守，因為它們會直接影響資金、稽核或資產狀態：

```text
ledger
balance
reservation
withdrawal
order
trade
outbox
audit
```

## 錯誤邊界

- 不要把 SQL、connection string、stack trace、secret 或 private key 帶到 API response。
- repository 層若遇到 constraint violation、connection failure 或 query failure，應轉成受控的 persistence exception。
- 上層 handler 只能回傳安全錯誤碼與去敏資訊。

## 文字圖

```text
+------------------+     +------------------+     +------------------+
| API / controller | --> | application      | --> | repository/port  |
+------------------+     +------------------+     +------------------+
                                                     |
                                                     v
                                               database client
```

## Phase 13 原則

- 先定義 boundary，再補具體 repository。
- 不新增真正 CRUD repository 實作。
- 不讓 persistence 直接輸出 API DTO。

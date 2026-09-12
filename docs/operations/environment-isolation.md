# 環境隔離

## 目的

將開發、測試、預備壓測與線上環境分開，避免測試 migration、壓測資料或本機 credential 誤寫入線上資料庫。環境名稱不是 production-ready 宣告；尤其 `ol` 仍受 P36 readiness gate 與人類簽核限制。

## 資料庫邊界

```text
dev   -> lumix_dev    本機開發資料；Compose 預設使用
test  -> lumix_test   自動測試專用；每輪測試開始先清除前一輪測試 schema
pre   -> lumix_pre    隔離的預備／壓測資料；禁止真實資金、客戶或 production secret
ol    -> 外部管理的 production database；不得使用本機 Docker volume 或 .env 密碼
```

`lumix_dev` 與 `lumix_test` 絕不可共用 database。test 可以在自己的 `lumix_test` 內建立短生命週期 schema，但不得在 `lumix_dev` 建立任何 `lumix_test_*` schema。

## 啟動規則

```text
dev  docker compose --env-file .env up --build -d
test ./server/mvnw test
pre  docker compose --project-name lumix-pre --env-file .env.pre up --build -d
ol   僅由受管部署平台與 secret manager 依受控變更流程部署
```

`pre` 必須使用獨立 Compose project、port、volume 與 `lumix_pre` database；不可指向 dev 的 PostgreSQL/Redis。`ol` 不提供本機 Compose 範本，以免將 localhost、明文 `.env` 或測試 volume 誤認為 production topology。

## 清理規則

- test JVM 首次使用測試 DataSource 時，清除 `lumix_test` 中上一輪的 `lumix_test_*` schema，然後建立本輪需要的隔離 schema。
- dev 與 pre 的清理須由人類明確要求，且必須先確認正確 database；不得以 test cleanup 邏輯處理。
- ol 資料不得由自動化 cleanup、Compose volume 操作或本機 agent 刪除。

## 線上邊界

`ol` 需要外部 PostgreSQL、Redis、TLS ingress、secret manager、backup/recovery、monitoring 與 P36 明確人類簽核。此文件只定義隔離邊界，不啟動線上服務，也不代表 production ready。

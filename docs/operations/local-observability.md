# 本機集中日誌（ELK）

## 架構與邊界

```text
Spring Boot ECS JSON rolling file
  -> runtime/logs/lumix-api.json
  -> Filebeat filestream
  -> Logstash Beats input
  -> Elasticsearch ILM index: lumix-logs-*
  -> Kibana Discover
```

此堆疊只供本機開發除錯，使用 Elastic Stack `9.5.7` 與 ECS Logging Java `1.8.0`。Elasticsearch 與 Kibana 只綁定 `127.0.0.1`；Logstash Beats input 只存在 Compose 內部網路。為降低本機啟動摩擦，Elasticsearch security 在此 profile 關閉，因此不得把這份設定直接用於正式環境或公開任何 Elastic port。

## 啟動

先啟動 API，讓 `runtime/logs/lumix-api.json` 開始產生，再執行：

```powershell
.\devops\observability\start-local-observability.ps1
```

腳本會：

1. 驗證 Compose 設定並建立被 Git 忽略的 `runtime/logs`。
2. 啟動 Elasticsearch，建立 `lumix-logs-policy` ILM policy。
3. 啟動 Logstash、Filebeat 與 Kibana。
4. 建立 `lumix-logs-*` Kibana data view。

入口：

- Kibana：`http://127.0.0.1:5601`
- Elasticsearch：`http://127.0.0.1:9200`
- API ECS JSON：`runtime/logs/lumix-api.json`

需要停止集中日誌但保留 index：

```powershell
docker compose --profile observability stop filebeat logstash kibana elasticsearch
```

不得在未確認資料保留需求前執行 `docker compose down --volumes`，因為這會刪除 PostgreSQL、Redis、Elasticsearch 與 Filebeat state volumes。

## 查詢方式

Kibana Discover 選擇 `lumix-logs-*`，常用欄位：

| 欄位 | 用途 |
| --- | --- |
| `request.id` | 與 API response 的 `X-Request-Id`、錯誤 body requestId 對照 |
| `trace.id` | 接受合法 W3C `traceparent`；沒有時由 API 建立 |
| `http.route` | Spring MVC route pattern，不含 query string |
| `http.response.status_code` | HTTP 結果 |
| `event.duration` | ECS 納秒單位處理時間 |
| `user.id_hash` | SHA-256 截短後的穩定使用者識別，不保存原始 user ID |
| `client.order.id` | 外部送單 clientOrderId |
| `external.source` | DEMO、未來 BINANCE 或其他 provider |
| `external.status_code` | provider-neutral 數字狀態碼 |
| `external.reason_code` | provider-neutral 數字原因碼 |

查詢例：

```text
request.id : "3f41..."
client.order.id : "client-order-123"
external.source : "DEMO" and external.status_code >= 1400
http.response.status_code >= 500
```

## 資料保護

`RequestCorrelationLoggingFilter` 不記錄 query、request／response body、Cookie、Authorization、session ID、email、原始 IP 或密碼。原始 user ID 與 client address 只保存截短 SHA-256，用於同一主體的問題關聯。交易服務新增業務欄位時只能記錄經驗證的識別碼、數字狀態與不含秘密的市場名稱；禁止寫入：

- 密碼、驗證碼、reset token、Cookie、API key、私鑰、助記詞或簽章內容。
- 完整信箱、電話、地址、裝置憑證或原始 IP。
- 任意 request body、第三方錯誤 response body 或資料庫連線字串。

正式環境另需 Elastic TLS、認證、RBAC、secret manager、網路 ACL、備份、容量與告警設計；本機 ILM 預設每天或 5 GB rollover，14 天後刪除，可用 `LUMIX_LOG_RETENTION_DAYS` 調整為 1–365 天。

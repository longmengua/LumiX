# Routes 總覽

## API

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8080` | `/actuator/health` | API health check |
| `8080` | `/api/**` | API endpoint |
| `8080` | `GET /api/v1/assets/balances` | 已登入使用者本人之唯讀 asset balance projection；不接受 userId／accountId 指定 owner |
| `8080` | `GET /api/v1/assets/accounts` | 已登入使用者本人之帳戶容器 inventory；不含餘額、projection 或帳本資料 |
| `8080` | `GET /api/v1/assets/history` | 已登入使用者本人之 immutable ledger entry 唯讀歷史；僅接受 bounded keyset cursor，不接受 userId／accountId 指定 owner |
| `8080` | `GET /api/v1/trading/{spot\|futures}/configuration` | 交易商品與有效政策配置；資料不足時不代表可接單 |
| `8080` | `GET /api/v1/trading/{spot\|futures}/workspace` | 已登入使用者的交易工作區摘要 |
| `8080` | `GET /api/v1/trading/{spot\|futures}/orders`、`GET .../{orderId}` | 本人委託列表與單筆委託 |
| `8080` | `POST /api/v1/trading/{spot\|futures}/orders` | 建立委託；要求登入、允許的 Origin、`Idempotency-Key`，且資料庫對應總開關為開啟 |
| `8080` | `DELETE /api/v1/trading/{spot\|futures}/orders/{orderId}` | 撤銷本人委託；總開關關閉時仍允許安全撤單 |
| `8080` | `GET /api/v1/trading/futures/reference` | 合約標記價／指數價參考；只作風控與顯示依據 |
| `8080` | `GET /api/v1/market-data/external-quote-preview-policy` | 公開外部報價預覽政策 |
| `8080` | `/api/v1/trading/external-proxy/demo/**` | loopback 與指定 Origin 才可使用的後端 Demo；不建立正式委託、成交或帳本 |
| `8080` | `GET /api/admin/v1/users/{userId}/assets` | 已啟用最高管理員之指定使用者 asset projection 唯讀檢視；沒有調帳／空投 command |
| `8080` | `GET /api/admin/v1/users/{userId}/assets/accounts` | 已啟用最高管理員之指定使用者帳戶容器唯讀檢視；不含餘額、不建立帳戶 |
| `8080` | `GET /api/admin/v1/users/{userId}/assets/history` | 已啟用最高管理員之指定使用者 immutable ledger 歷史唯讀檢視；最多 20 筆，沒有調帳／空投 command |
| `8080` | `GET /api/admin/v1/trading/runtime-settings` | 最高管理員讀取 SPOT／FUTURES 資料庫接單總開關 |
| `8080` | `PUT /api/admin/v1/trading/runtime-settings/{kind}` | 以 `expectedVersion` 更新接單總開關並寫入稽核紀錄 |
| `8080` | `GET`／`PUT /api/admin/v1/trading/external-order-proxy/settings` | 讀寫資料庫 Demo Proxy 開關；預設開啟 |
| `8080` | `/api/admin/v1/trading/**` | 現貨／合約政策、商品狀態、funding、強平與外部報價管理命令；皆要求最高管理員及對應安全標頭 |
| `8080` | 所有 HTML route，例如 `/login`、`/admin/login` | `404` |

## 前台

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8090` | `/` | 前台首頁 |
| `8090` | `/login` | 用戶登入 |
| `8090` | `/register` | 用戶註冊 |
| `8090` | `/forgot-password` | 用戶忘記密碼 |
| `8090` | `/account/**` | 用戶帳戶頁面 |
| `8090` | `/assets/**` | 用戶資產頁面 |
| `8090` | `/api/v1/**` | 前台 API proxy |
| `8090` | `/admin`、`/admin/**`、`/api/admin/**` | `404` |

## 後台

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8100` | `/`、`/admin` | 導向 `/admin/login` |
| `8100` | `/admin/login` | 管理員登入 |
| `8100` | `/admin/forgot-password` | 管理員忘記密碼 |
| `8100` | `/admin/reset-password` | 管理員密碼重設 |
| `8100` | `/admin/users`、`/admin/account/**` | 管理端頁面 |
| `8100` | `/api/admin/v1/**` | 管理端 API proxy |
| `8100` | `/login`、`/register`、`/api/v1/**`、其他非 `/admin` HTML route | `404` |

以上為 `.env` 預設內部埠。本機 HTTPS gateway 預設以 `https://localhost` 提供前台，並以 `https://admin.localhost` 提供後台；兩個 hostname 維持頁面與 API 隔離。

## 本機觀測

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `5601` | `/` | Kibana；只綁定 loopback，不經前後台 Gateway 對外公開 |
| `9200` | `/` | Elasticsearch；只供本機 observability profile 使用 |

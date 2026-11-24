# Routes 總覽

## API

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8080` | `/actuator/health` | API health check |
| `8080` | `/api/**` | API endpoint |
| `8080` | `GET /api/v1/assets/balances` | 已登入使用者本人之唯讀 asset balance projection；不接受 userId／accountId 指定 owner |
| `8080` | `GET /api/v1/assets/accounts` | 已登入使用者本人之帳戶容器 inventory；不含餘額、projection 或帳本資料 |
| `8080` | `GET /api/v1/assets/history` | 已登入使用者本人之 immutable ledger entry 唯讀歷史；僅接受 bounded keyset cursor，不接受 userId／accountId 指定 owner |
| `8080` | `GET /api/admin/v1/users/{userId}/assets` | 已啟用最高管理員之指定使用者 asset projection 唯讀檢視；沒有調帳／空投 command |
| `8080` | `GET /api/admin/v1/users/{userId}/assets/accounts` | 已啟用最高管理員之指定使用者帳戶容器唯讀檢視；不含餘額、不建立帳戶 |
| `8080` | `GET /api/admin/v1/users/{userId}/assets/history` | 已啟用最高管理員之指定使用者 immutable ledger 歷史唯讀檢視；最多 20 筆，沒有調帳／空投 command |
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

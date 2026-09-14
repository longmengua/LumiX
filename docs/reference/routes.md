# Routes 總覽

## API

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8080` | `/actuator/health` | API health check |
| `8080` | `/api/**` | API endpoint |
| `8080` | `GET /api/v1/assets/balances` | 已登入使用者本人之唯讀 asset balance projection；不接受 userId／accountId 指定 owner |
| `8080` | 所有 HTML route，例如 `/login`、`/admin/login` | `404` |

## 前台

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8088` | `/` | 前台首頁 |
| `8088` | `/login` | 用戶登入 |
| `8088` | `/register` | 用戶註冊 |
| `8088` | `/forgot-password` | 用戶忘記密碼 |
| `8088` | `/account/**` | 用戶帳戶頁面 |
| `8088` | `/api/v1/**` | 前台 API proxy |
| `8088` | `/admin`、`/admin/**`、`/api/admin/**` | `404` |

## 後台

| Port | Route | 用途／預期行為 |
| --- | --- | --- |
| `8089` | `/`、`/admin` | 導向 `/admin/login` |
| `8089` | `/admin/login` | 管理員登入 |
| `8089` | `/admin/forgot-password` | 管理員忘記密碼 |
| `8089` | `/admin/reset-password` | 管理員密碼重設 |
| `8089` | `/admin/users`、`/admin/account/**` | 管理端頁面 |
| `8089` | `/api/admin/v1/**` | 管理端 API proxy |
| `8089` | `/login`、`/register`、`/api/v1/**`、其他非 `/admin` HTML route | `404` |

# Page Map

```text
/                 landing
/login            login
/register         register
/dashboard        account overview
/trade/:market    spot trading page
/orders           open orders and history
/wallet           balances
/wallet/deposit   deposit instructions
/wallet/withdraw  withdrawal request
/admin/login      後台專用登入入口；由 admin-web 獨立 port 發佈，不提供自助註冊
/admin/forgot-password
                 後台專用密碼復原申請；維持通用回應，且只允許 ACTIVE 管理員 principal
/admin/reset-password
                 後台一次性密碼重設連結目的地
/admin            操作員儀表板（必須通過管理員 principal 驗證）
```

## 前後台入口隔離不變式

- LumiX 必須維持三個獨立 service 與 port：`server:8080`、`web:8088`、`admin-web:8089`。不得為了部署便利把三者合併為單一 service、單一 SPA 或單一公開入口。
- `server:8080` 僅提供 API 與 health endpoint，絕不可提供前台或管理端 HTML；`/admin` 與 `/admin/*` 的唯一合法本機入口是 `admin-web:8089`。
- 前台必須由 `web:8088` 發佈，且只處理一般用戶路由；它不可提供 `/admin` 或 `/admin/*` route。
- 管理端必須由 `admin-web:8089` 發佈，且只提供 `/admin/*` route 與必要靜態資產；它不可提供前台路由。
- 兩端必須分別 build `VITE_LUMIX_SURFACE=client` 與 `VITE_LUMIX_SURFACE=admin`，不可改回單一 bundle 再依 browser path 決定掛載哪個 App。
- 本機 Vite 開發也必須分別使用 `npm run dev:client`（8088）與 `npm run dev:admin`（8089）；不得恢復以同一 Vite server 依 `/admin` path 掛載兩個 App 的作法。
- 前台 session cookie 為 `LUMIX_SESSION`、管理端 session cookie 為 `LUMIX_ADMIN_SESSION`；管理 API 只能接受後者，不能把一般使用者 session 視為管理登入。
- 前台 origin 不得 reverse proxy `/api/admin/*`，管理端 origin 不得 reverse proxy `/api/v1/*`；不可以只靠前端畫面隱藏舊登入流程。
- 未來變更 Compose、Nginx、Vite entry 或 authentication cookie 前，必須維持上述隔離並加入相應驗證；不得以共同 ingress、path rewrite 或 reverse proxy 為由，把三者重新指向同一 SPA service。
- 三個 container 的隔離只處理路由、程序與權限邊界；正式環境若要降低單一攻擊或故障造成全站不可用的風險，必須為三者配置獨立 runtime／資源限制／健康檢查／擴縮策略，並評估資料庫、Redis、網路與主機等共享故障域。

## Trading page responsibilities

```text
market selector
order book
price chart
order form
open orders
recent trades
balance preview
risk / error display
```

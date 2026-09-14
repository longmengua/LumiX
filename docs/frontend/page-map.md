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

- 前台必須由 `web` service 發佈，預設 port 為 `8088`；它不可提供 `/admin/*` route。
- 管理端必須由 `admin-web` service 發佈，預設 port 為 `8089`；它只提供 `/admin/*` route 與必要靜態資產。
- 兩端必須分別 build `VITE_LUMIX_SURFACE=client` 與 `VITE_LUMIX_SURFACE=admin`，不可改回單一 bundle 再依 browser path 決定掛載哪個 App。
- 前台 session cookie 為 `LUMIX_SESSION`、管理端 session cookie 為 `LUMIX_ADMIN_SESSION`；管理 API 只能接受後者，不能把一般使用者 session 視為管理登入。
- 前台 origin 不得 reverse proxy `/api/admin/*`，管理端 origin 不得 reverse proxy `/api/v1/*`；不可以只靠前端畫面隱藏舊登入流程。
- 未來變更 Compose、Nginx、Vite entry 或 authentication cookie 前，必須維持上述隔離並加入相應驗證；若需要共同 ingress，必須在 ingress 層保留兩個獨立 host／route boundary，不可重新指向同一 SPA service。

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

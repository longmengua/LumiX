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
/admin/login      後台專用登入入口；不提供自助註冊
/admin/forgot-password
                 後台專用密碼復原申請；維持通用回應，且只允許 ACTIVE 管理員 principal
/admin/reset-password
                 後台一次性密碼重設連結目的地
/admin            操作員儀表板（必須通過管理員 principal 驗證）
```

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

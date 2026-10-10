# 文件位置規範

## 目的

LumiX 以文件責任邊界管理正式文件：跨服務架構與治理在 repository root `docs/`，實作細節由各服務在自己的 `docs/` 維護。

## 強制規則

- 跨服務架構、治理、規劃、產品、營運、exchange-core 與共用工程規範一律放在 root `docs/` 的既有主題子目錄。
- Java / Spring API、資料庫 migration、後端模組、交易邊界與後端測試策略放在 `server/docs/`。
- React / TypeScript 前端頁面、元件、token、UX 與前端狀態管理文件放在 `web/docs/`。
- 需要新增分類時，先更新擁有該分類的 `README.md` 路由，再建立文件。
- 禁止在 `src/`、`resources/`、migration 目錄存放 README、設計規格或操作手冊。
- 搬遷文件時必須修正 repository 內的引用，並以 Git rename 保留歷史。

## 分類

```text
docs/governance/      權威規則、風險與門檻
server/docs/          後端 API、資料庫、模組、測試與交易邊界
web/docs/             前端頁面、元件、token、UX 與狀態管理
docs/engineering/     跨服務工程規範
docs/exchange-core/   帳本、錢包、風控及交易核心
docs/ai/              AI agent 工作規則
```

## 驗證

每次新增或搬遷文件後，至少確認：

- 文件位於其責任邊界：跨服務文件在 root `docs/`，後端在 `server/docs/`，前端在 `web/docs/`。
- source/resource 目錄未遺留正式文件。
- 所有舊路徑的文字引用均已更新。
- root `docs/README.md` 或擁有服務的 `README.md` 可找到新文件。

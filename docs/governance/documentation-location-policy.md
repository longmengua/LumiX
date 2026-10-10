# 文件位置規範

## 目的

LumiX 的正式文件只在 repository root 的 `docs/` 管理，避免 server、web 與部署目錄各自產生無法同步的規格副本。

## 強制規則

- 新增或搬遷的正式文件一律放在 `docs/` 的既有主題子目錄；若無合適類別，先更新 `docs/README.md` 與對應子目錄 `README.md` 的路由，再建立新目錄。
- 禁止建立或回填 `server/docs/`、`web/docs/`、`admin-web/docs/`，也不得在 `src/`、`resources/`、migration 目錄存放 README、設計規格或操作手冊。
- 程式碼、設定與 migration 以 root `docs/` 的文件作為說明來源；文件內可引用其實際程式路徑，但不得複製一份規格到服務目錄。
- 服務根目錄不得保留 README、設計規格或操作手冊；服務入口與建置說明同樣放在 root `docs/`。
- 搬遷文件時必須修正 repository 內的引用，並以 Git rename 保留歷史。

## 分類

```text
docs/governance/      權威規則、風險與門檻
docs/backend/         後端 API、模組、資料存取與交易邊界
docs/database/        Flyway migration 與資料庫慣例
docs/engineering/     工程與測試規範
docs/exchange-core/   帳本、錢包、風控及交易核心
docs/ai/              AI agent 工作規則
```

## 驗證

每次新增或搬遷文件後，至少確認：

- `server/docs/`、`web/docs/` 與 source/resource 目錄未遺留正式文件。
- 所有舊路徑的文字引用均已更新。
- 根目錄 `docs/README.md` 與目標子目錄 `README.md` 可找到新文件。

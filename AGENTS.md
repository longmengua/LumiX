# LumiX 開發共用規範

本檔是 AI agent 的專案入口。開始工作時先讀本檔，再依任務閱讀相關程式碼與規格。

## 專案目標與現況

LumiX 以可審計、可維運的交易所為目標。已存在的功能須以程式碼、設定與實際驗證判斷。文件或測試通過不代表正式上線就緒。正式上線仍須符合 `docs/governance/PRODUCTION_READINESS_GATES.md` 並取得人類明確簽核。

## 開發方式

- 依使用者需求與實際技術相依施工，變更範圍保持明確。閱讀直接相關的文件，避免一次載入全部歷史文件。
- 重用既有模型、服務與共用元件。變更後執行最窄且足夠的驗證，回報實際結果、未驗證項目與限制。
- 不以 TODO、placeholder、mock、固定餘額、假地址、假交易或假成功狀態冒充完成。測試 fixture 留在測試 source set。
- 新增或修改的程式碼遵守 `docs/engineering/code-commenting-standard.md`，以繁體中文註解說明業務、安全及資料一致性原因。
- 提交前檢查 `git status --short`。只有工作目錄全是本任務變更時才使用 `git add .`，否則明確指定 staging 路徑。

## 技術與資料邊界

- 後端使用 Java 21、Spring Boot 3，前端使用 React、TypeScript、Vite。
- 第三方服務與資料由後端接入。前端只連 LumiX 內部 API 或推播，不持有第三方憑證。
- API、前台、後台維持獨立服務與埠 8080、8088、8089。本機 Docker Compose 只啟動 PostgreSQL、Redis、Kafka，其餘以主機程序啟動。
- 金額、價格、數量及費用使用整數最小單位或受控精度 decimal，避免 binary floating point。
- 帳本採不可變 append-only 記錄，修正以追加 reversal 或 adjustment 表示。資金、交易與管理命令須維持冪等、權限隔離、可追蹤稽核、對帳及失敗即拒絕。
- 錢包 provider、私鑰、簽章、廣播或資金移動路徑須具備來源、secret isolation、權限審核、不可變稽核、冪等、對帳及相應 runtime 驗證後才能啟用。
- 修改交易、帳本、錢包、風控、認證、授權或 secret 管理時，回報須標記 `HUMAN_REVIEW_REQUIRED` 與具體審核重點。此標記用於交付審核。

## 文件與 UI

- 文件以繁體中文為主。架構、流程與資料圖使用純文字圖。
- 跨服務規格放在 `docs/` 對應主題，後端實作文件放 `server/docs/`，前端實作文件放 `web/docs/`。
- UI 工作先讀 `docs/ai/LUMIX_UI_GOVERNANCE.md`、`docs/ai/LUMIX_UI_IMPLEMENTATION.md` 及相關共用元件。設計以 `docs/governance/lumix-institutional-blue.md` 為準，修改共用視覺規則時同步相關文件。

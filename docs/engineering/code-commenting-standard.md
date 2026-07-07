# 程式碼註解規範

此文件定義 LumiX 全專案的程式碼註解標準。目標不是把每一行都寫滿註解，而是讓後續維護者能快速看懂為什麼要這樣做，以及哪些地方不能隨便改。

## 核心原則

- 所有新增或修改的程式碼都必須具備足夠註解，以利後續維護。
- 註解必須使用繁體中文，採台灣用語。
- 標準技術術語保留英文，例如 migration、ledger、reservation、matching、settlement、idempotency、outbox。
- 不要求每一行都加註解。
- 不寫重複程式碼內容的噪音註解。
- 註解應優先說明為什麼這樣做，而不是重述程式碼表面行為。
- 高風險提醒不應直接刪除；若原本是 `TODO` / `placeholder`，請改寫成繁體中文維護註解，並保留 `TODO(HUMAN_REVIEW_REQUIRED)`、`placeholder` 或 `not production-ready` 這類風險語意。

## 註解應說明的內容

- 為什麼這樣做。
- 業務限制。
- 安全限制。
- 資料一致性限制。
- 後續維護注意事項。

## 高風險領域

下列領域的註解要更完整，因為它們會直接影響資金、審計或系統安全：

- ledger
- balance
- reservation
- withdrawal
- matching
- settlement
- PnL
- liquidation
- risk
- admin
- security
- idempotency
- outbox
- reconciliation

## SQL migration 規則

- migration 檔頭要說明這份 migration 的目的與邊界。
- table / column 必須有 inline comments，讓 schema 本身可讀。
- PostgreSQL migration 必須補 `COMMENT ON TABLE` 與 `COMMENT ON COLUMN`。
- 高風險 schema 要更清楚標示約束、查詢意圖與後續重建注意事項。

## Java 程式規則

- `public class` 應有類別層級說明。
- `public method` 應有方法層級說明，尤其是輸入驗證、回傳語意與副作用邊界。
- 複雜 business rule 要有區塊註解，說明規則來源與不能簡化的原因。
- transaction boundary 要說明為什麼要這樣切分，以及哪些步驟必須同交易完成。
- 高風險流程要清楚標示安全限制與資料一致性限制。

## TypeScript / React 規則

- 複雜 hook 要說明狀態來源、同步順序與副作用邊界。
- 狀態流要說明為什麼不能簡化，以及哪些狀態必須保留。
- 權限判斷要說明授權假設與失敗路徑。
- 交易表單驗證要說明風險、精度與 API 假設。
- API boundary 要說明 request / response 的一致性與錯誤處理。

## 測試規則

- 測試要註解測試意圖，不只是測試步驟。
- 資金、安全、資料一致性與錯誤案例要特別說明為什麼這個 case 必須存在。
- 若測試是在保護不變式，註解應直接指出不變式名稱或限制。

## 何時應優先重構

- 如果需要大量註解才能理解程式，先考慮拆小函式或重構責任邊界。
- 註解不能掩蓋壞設計。
- 註解也不能取代命名不良、函式過長、責任混雜或交易邊界模糊的問題。

## AI 執行要求

- AI 在完成任何任務時，review summary 必須說明 comments 是否完成。
- 若新增或修改程式碼，必須檢查是否有足夠註解支撐後續維護。
- 若是高風險變更，註解與文件要一起被視為交付的一部分。

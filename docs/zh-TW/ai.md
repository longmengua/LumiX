<!-- 檔案用途：繁體中文 AI 文件索引。英文版本位於 ../en/ai.md。 -->
# AI 文件

English version：[../en/ai.md](../en/ai.md)

這頁連到給 Codex / 代理使用的精簡地圖。共用原始檔位於 `docs/ai/`，讓 Codex 依任務只讀需要的 map。

## 入口

| 文件 | 說明 |
| --- | --- |
| [AI README](../ai/README.md) | 說明如何用 Markdown 文件當 Codex 任務入口。 |
| [Code Map Index](../ai/code-map.md) | 依任務分類的 code map 目錄。 |

## 程式註解標準

AI 產生或修改的程式碼都應加入有助於理解的註解，讓讀者能快速看懂業務意圖與測試流程。註解應優先說明狀態轉換、replay / recovery 行為、帳務影響、風控判斷、不變量與邊界情境。測試程式應透過註解或 `@DisplayName` 說清楚 setup、action、expected result。

避免只重複語法本身的雜訊註解。

## Code Maps

| 範圍 | 連結 |
| --- | --- |
| 下單與撮合 | [../ai/maps/order-matching.md](../ai/maps/order-matching.md) |
| 風控、帳務與資金 | [../ai/maps/risk-ledger-funds.md](../ai/maps/risk-ledger-funds.md) |
| 可靠性與行情 | [../ai/maps/reliability-market-data.md](../ai/maps/reliability-market-data.md) |
| Polymarket 與安全 | [../ai/maps/polymarket-security.md](../ai/maps/polymarket-security.md) |
| 做市商與對沖 | [../ai/maps/market-maker-hedging.md](../ai/maps/market-maker-hedging.md) |
| Web 應用 | [../ai/maps/web-apps.md](../ai/maps/web-apps.md) |
| Persistence 與測試 | [../ai/maps/persistence-tests.md](../ai/maps/persistence-tests.md) |

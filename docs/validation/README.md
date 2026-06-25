<!-- 檔案用途：驗證文件入口，收斂產品功能驗收表與回歸檢查矩陣。 -->
# Validation

這個目錄放「要證明交易所功能可用」的驗收與回歸文件。

| 文件 | 用途 |
| --- | --- |
| [exchange-function-validation.md](exchange-function-validation.md) | 完整交易所功能驗證表，覆蓋前台、API、撮合、帳務、風控、後台、做市商、Polymarket、訊息與運維驗證。 |

## 使用方式

- 每個 release、demo、重大重構後，從 [exchange-function-validation.md](exchange-function-validation.md) 選本次受影響的範圍驗證。
- `Status` 欄位建議使用 `todo`、`pass`、`fail`、`blocked`、`n/a`。
- `Evidence` 欄位放測試名稱、curl 腳本、截圖、WebSocket event、log id、commit 或 runbook 記錄。

# test domain/util

Domain utility tests。

目前內容：

| 測試類別 | 主要案例 |
| --- | --- |
| `OrderBookChecksumTest` | 等價 BigDecimal 不因 scale 不同產生不同 checksum。 |
| `SensitiveLogSanitizerTest` | query string、key=value、JSON body、Authorization header、known sensitive headers 遮罩。 |

注意：
- 安全或 market-data 相關工具要保持 deterministic test coverage。

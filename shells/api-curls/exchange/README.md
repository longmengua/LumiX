# exchange curl scripts

這裡放內部交易所 API 的 curl 範例。

目前狀態：
- 訂單：下單、查 open/all、撤單、amend、cancel-replace 的主要 API 已在 controller 中提供；此目錄目前只放部分常用範例。
- 行情：depth snapshot 與 depth delta backfill 範例已補齊。
- 資金/風控：入金、出金、margin transfer、transfer list、bonus-credit user/campaign report/clawback、turnover summary/records/reconciliation、risk snapshot、persisted risk snapshot、price oracle、ADL queue claim/release/execution 範例已補齊。
- Recovery：包含 snapshot recovery 與全帳戶 reconciliation 範例。

新增 API 時請同步補：
1. 對應 curl 腳本。
2. `../README.md` 和 `../README_ch.md` 的腳本列表。
3. `docs/*/README.md` 的主要 API 列表。

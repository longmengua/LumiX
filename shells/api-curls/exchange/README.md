# exchange curl scripts

這裡放內部交易所 API 的 curl 範例。

目前狀態：
- 訂單：下單、查 open/all、撤單、amend、cancel-replace 的主要 API 已在 controller 中提供；此目錄目前只放部分常用範例。
- 行情：depth snapshot 與 depth delta backfill 範例已補齊。
- 資金/風控：入金、出金、margin transfer、transfer list、bonus-credit user/campaign report/export/clawback、turnover summary/records/export/reconciliation、risk snapshot、persisted risk snapshot、price oracle、ADL queue claim/release/execution/alerts、ADL execution report、ADL insurance reconciliation、insurance fund movement 範例已補齊。
- 做市商：post-only quote placement、active quote state 查詢與 quote/open-order reconciliation 範例已補齊。
- Admin：market config 與 risk parameters read-only 查詢範例已補齊。
- Recovery：包含 snapshot recovery、全帳戶 reconciliation、account-position consistency、daily/category finance report、category export batch、ledger archive delete guard、manifest restore smoke、replay validation、outbox/domain-state consistency 範例。

新增 API 時請同步補：
1. 對應 curl 腳本。
2. `../README.md` 和 `../README_ch.md` 的腳本列表。
3. `docs/*/README.md` 的主要 API 列表。

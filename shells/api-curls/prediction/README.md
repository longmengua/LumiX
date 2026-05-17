# prediction curl scripts

這裡放 Polymarket / prediction market 整合 API 的 curl 範例。

目前狀態：
- market discovery / sync / retry / reset / price refresh。
- session signer init / confirm / list / revoke。
- CLOB API key create / derive。
- local order place / cancel / sync / reconcile。
- user WebSocket start / stop / status。

注意：
- 執行前先設定 `application-dev.yml` 或環境變數中的 Polymarket wallet / CLOB 參數。
- 這些腳本不應包含真實私鑰或 API credentials。

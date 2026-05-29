# interfaces/web/dto

REST API request/response DTO。

目前狀態：
- 內部交易所：下單、改單、cancel-replace、margin deposit/withdraw/transfer、order info、risk price oracle、ADL queue claim/release/execution、matching worker readiness、reconciliation issue workflow、market-maker profile/risk-limit request、hedge venue fill callback。
- Polymarket：session、market、outcome、order、approval、WS status。
- 共用：`ApiResponse`。

注意：
- DTO 應保留 Bean Validation 註解。
- DTO 轉 application command 的方法應明確說明 path/body 欄位來源。

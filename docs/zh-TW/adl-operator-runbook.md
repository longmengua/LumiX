<!-- 檔案用途：ADL queue claim、partial retry、no-candidate retry 與 reconciliation 的營運手冊。英文版本：../en/adl-operator-runbook.md。 -->
# ADL 營運手冊

這份手冊處理清算缺口超過保險基金承接能力後產生的 ADL queue。

## 檢查

1. 查 open ADL shortfall：`GET /api/risk/adl-queue`。
2. 查卡住的 claim：`GET /api/risk/adl-queue/stuck-claims?minClaimAgeSeconds=900`。
3. 查可送 alert backend 的 open/stuck queue entries：`GET /api/risk/adl-queue/alerts?minAgeSeconds=900`。
4. 查最近執行結果：`GET /api/risk/adl-executions?limit=50`。
5. 查 insurance-fund capital movements：`GET /api/risk/insurance-fund/movements?asset=USDT&limit=50`。
6. 對帳 ADL queue 與被清算持倉 coverage：`GET /api/risk/adl-insurance-reconciliation?asset=USDT`。

## Claim And Execute

1. 手動執行前先 claim：`POST /api/risk/adl-queue/{liquidationId}/claim`。
2. 用唯一 `commandId` 執行：`POST /api/risk/adl-queue/{liquidationId}/execute`。
3. 只有在重試同一次不確定執行時才重用同一個 `commandId`；durable execution record 會讓已完成 command replay-safe。

## Partial Retry

如果只部分執行，queue amount 會更新成 `remainingNotional`。後續只針對同一個 `liquidationId` retry，不要手動補建另一筆 shortfall。

## No Candidate Retry

`ADL_NO_ELIGIBLE_CANDIDATES` 會保留原 queue 不變。等出現有獲利的反向持倉後再 retry，或由風控營運決定其他人工處理方式。

## Stuck Claims

如果 owner 無法處理，先用目前 owner 呼叫 `POST /api/risk/adl-queue/{liquidationId}/release`，再由替代 operator claim。新的執行嘗試要使用新的 `commandId`，保留清楚 audit trail。

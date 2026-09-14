# Phase 30 - 前端正式交易 UX

## 狀態

```text
COMPLETED_FOR_TRUSTED_UX_PRESENTATION_FOUNDATION
```

## Phase charter

建立可信 API contract、health 與安全狀態的 UX presentation foundation；目前 React mock/development adapter 不得被誤接或宣稱為正式交易 UI。

## 高層任務

1. Design system、accessibility、locale、asset/price precision 與 error presentation contract：`COMPLETED_FOR_TRUSTED_STATE_COMPONENT`。
2. Market/order/account/wallet user journey：`COMPLETED_FOR_STATE_PRESENTATION`；明確呈現 live/stale/degraded/sandbox/unavailable。
3. Sensitive operation UX：`COMPLETED_FOR_ENABLEMENT_GATE`；只有 live 可啟用。
4. API/WebSocket adapter boundary：`COMPLETED_FOR_BOUNDARY`；不在 browser 計算資金真相、未接 mock。
5. E2E/accessibility/security regression：`COMPLETED_FOR_TYPECHECK`；production E2E 屬後續 runtime gate。

## 2026-09-14 後台登入介面收斂

`/admin/login` 已改為獨立的深色 Enterprise Admin Portal presentation：使用既有的 admin principal 驗證、CAPTCHA、HttpOnly session、i18n 與忘記密碼路由，不新增 SSO 或替代認證 API。頁面提供鍵盤可操作的密碼顯示切換、Caps Lock 提示、ARIA live 錯誤區與 desktop/mobile 響應式版面；SSO 因尚無 OAuth/OIDC/SAML runtime 而刻意不呈現可點擊的假按鈕。此變更只改善 trusted UX presentation，不代表 P29/P30 已具 production runtime 或 production-ready 狀態。

## 2026-09-15 ASSET-T02 唯讀資產 Projection 呈現

`/assets`、`/assets/spot`、`/assets/futures` 與 `/assets/margin` 已接入 authenticated owner scoped 的
`GET /api/v1/assets/balances`。browser 保留十進位字串精度，並將 source、projection 時間與 reconciliation
freshness 呈現為 evidence；沒有 materialized row 時顯示 empty state，不會製造零餘額或 mock asset。因尚無可驗證的
價格、資產歷史、劃轉／reservation 或 PnL runtime，這些頁面刻意不顯示估值、總權益、PnL、操作按鈕或假歷史。
此 handoff 只完成 read-only projection presentation，不代表 ledger truth、資金異動或正式資產營運已完成。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；P29 contract、P26 risk、P27 admin policy 與 P28 audit evidence 未通過前，不得啟用 production-affecting UI flow。

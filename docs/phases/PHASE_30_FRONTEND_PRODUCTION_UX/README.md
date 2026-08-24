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

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；P29 contract、P26 risk、P27 admin policy 與 P28 audit evidence 未通過前，不得啟用 production-affecting UI flow。

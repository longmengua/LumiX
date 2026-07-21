# Phase 30 - 前端正式交易 UX

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

以可信 API contract、精度、health 與安全狀態為前提完成正式 UX；目前 React mock/development adapter 不得被誤接或宣稱為正式交易 UI。

## 高層任務

1. Design system、accessibility、locale、asset/price precision 與 error presentation contract。
2. Market/order/account/wallet user journey，明確呈現 stale、degraded、pending、rejected、review-required 狀態。
3. Sensitive operation UX：withdrawal destination confirmation、MFA/approval、risk warning、idempotent retry 與不可逆操作提示。
4. API/WebSocket adapter boundary、reconnect/resync、client-side data minimization；不在 browser 計算資金真相。
5. E2E/accessibility/security regression、mock removal verification 與 rollback feature control。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；P29 contract、P26 risk、P27 admin policy 與 P28 audit evidence 未通過前，不得啟用 production-affecting UI flow。

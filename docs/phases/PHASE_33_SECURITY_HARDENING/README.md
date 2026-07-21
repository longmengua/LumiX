# Phase 33 - 安全強化

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

以 threat model、defence-in-depth 與可驗證 remediation 取代安全宣稱；安全 control 不得以 convenience 或 sandbox 需求 bypass。

## 高層任務

1. 系統/資產/信任邊界 threat model 與風險登錄。
2. Authentication、authorization、MFA、session/API key、admin/signer separation 與 least privilege。
3. Secret/key management、supply-chain/dependency、configuration hardening、secure logging 與 vulnerability response。
4. Application/network/data protection、abuse resistance、penetration test 與 remediation verification。
5. Security incident runbook、evidence、exception expiry 與 independent human review。

## Gate

所有 security runtime 與 exception 均 `HUMAN_REVIEW_REQUIRED`；未關閉或正式接受的重大風險、未完成 signer/withdrawal security review 時不得 launch。

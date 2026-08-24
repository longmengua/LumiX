# P25-T01 提款審核與職責分離

## 範圍

定義可重放的 immutable approval policy，消費 P24 已經對帳的 keyless signer input，產生是否可進入下一個 signing-intent contract 的純決策。

## 不變式

- request owner 不可審核自己的提款。
- 每名 approver 只可提供一個角色證據；必須覆蓋 requirement 所要求的不同角色。
- 超過 atomic amount 限額或超過審核有效期限，一律拒絕。
- policy 只檢查 caller 提供的 authorization evidence identity/version，不實作登入、權限查詢、admin bypass 或角色授與。
- approved result 不是簽章授權、私鑰存取或 broadcast 命令。

## 禁止事項

不得新增 account 權限變更、database、secret、HSM/MPC、signer adapter、transaction payload、nonce/UTXO 選擇、RPC/provider 或鏈上廣播。

## 驗收

測試 self-approval、同一人重複審核、角色不足、限額/expiry 與符合雙角色 requirement 的 deterministic outcome。所有結果必須為 immutable evidence，且不含敏感資料。

## 風險

`HUMAN_REVIEW_REQUIRED: yes`；審查重點為 reviewer identity 的輸入邊界、角色分離、限額與 expiry 的 fail-closed 語意。

# 資產寫入 Runtime 前置順序

## 目的

本文件將 ASSET-T04 至 ASSET-T10 所需的執行服務依賴固定為不可跳過的施工順序。人類已授權全鏈資產 runtime 施工；鏈上連線、私鑰、簽章與廣播只能在具名 provider、secret isolation、權限審核、immutable audit、idempotency、reconciliation 與失敗即拒絕的條件全部具備後，依本文件順序啟用。

## 依賴順序

```text
P14 帳本入帳服務驗證證據
        |
        v
P15 餘額投影更新＋資產保留＋對帳服務驗證證據
        |
        +-----------------------+
        |                       |
        v                       v
ASSET-T04 internal transfer   ASSET-T10 governed airdrop
        |                       |
        +-----------+-----------+
                    |
                    v
P22 address / observation provider runtime
                    |
                    v
P23 deposit eligibility / credit / reversal runtime
                    |
                    v
P24 withdrawal request / hold runtime
                    |
                    v
P25 signer / broadcast / confirmation runtime
```

## 每個執行服務的最低驗收證據

| 順序 | 前置執行服務 | 必要證據 | 未通過時行為 |
| --- | --- | --- | --- |
| 1 | 帳本入帳 | 只追加的日誌與分錄、借貸平衡、冪等性、交易回復、請求與稽核關聯 | 拒絕任何資產命令 |
| 2 | 餘額投影更新 | 入帳後可重現的重建、數量精度、延遲與新鮮度、對帳差異 | 停止對外宣稱可用餘額 |
| 3 | 資產保留 | 保留／解除／扣取的擁有者、資產與原子數量一致性及冪等性 | 拒絕劃轉、提款與空投 |
| 4 | 帳戶所有權檢查／權限審核／稽核紀錄 | 凍結帳戶的對外轉出或轉給他人必須拒絕、具權限操作者、原因、不可變證據 | 拒絕不符合權限的特權資產命令 |
| 5 | 內部劃轉 | 來源與目的帳戶同一擁有者、單一交易、雙分錄、餘額投影／對帳證據 | 不提供劃轉介面或程式介面 |
| 6 | 空投 | 活動資格、資產狀態、每位使用者／活動冪等性、權限審核、不可變稽核證據 | 不提供空投 UI/API |
| 7 | Deposit | address ownership、provider health/finality、reorg、credit/reversal idempotency | 不顯示入金地址或 credited balance |
| 8 | Withdrawal | request/hold、risk、approval、keyless signer intent、broadcast/confirmation reconciliation | 不提供提現 UI/API |

## 明確禁止的捷徑

- 直接 `UPDATE balance_projections` 或 `INSERT ledger_entries` 作為管理員空投。
- 由 browser 送入可指定任意 owner/account/amount 的資產 command。
- 用 mock、固定餘額、假地址、假 txid 或假成功狀態補足缺少 runtime。
- 未經資產保留、風險控管與稽核就將資產標示為可用。

## 帳戶凍結範圍

帳戶凍結不是全面禁止資產異動。凍結帳戶不可轉出給其他使用者，也不可轉到外部地址；同一使用者自己名下的現貨、合約與槓桿帳戶之間可正常轉入與轉出。空投不屬於使用者發起的轉出，因此可轉入凍結帳戶。金額上限、頻率、風險評分、地區、裝置或行為模型均不在本輪施工範圍。

## 人工審核

所有上述第 1 至 8 項都是 `HUMAN_REVIEW_REQUIRED`。在 evidence 不完整時，正確行為是 fail-closed；不得以
「內部測試」或「管理員操作」降低資金安全門檻。

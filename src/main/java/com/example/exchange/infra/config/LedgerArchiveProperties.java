/*
 * 檔案用途：ledger archive/delete eligibility policy 設定。
 */
package com.example.exchange.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ledger.archive")
public class LedgerArchiveProperties {

    /**
     * hot ledger 最少保留天數；低於此 cutoff 的資料才可能進入 archive/delete eligibility。
     */
    private long hotRetentionDays = 365L;

    /**
     * true 時 delete eligibility 必須先通過 hash-chain tamper-evidence 驗證。
     */
    private boolean requireTamperEvidenceClean = true;

    /**
     * true 時 delete eligibility 必須確認該 UTC 財務日報借貸平衡。
     */
    private boolean requireBalancedDailyReport = true;
}

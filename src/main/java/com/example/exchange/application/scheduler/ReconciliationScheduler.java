/*
 * 檔案用途：應用層排程任務，定期驅動快照、資金費、對帳或 Polymarket 同步。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.ReconciliationService;
import com.example.exchange.domain.model.dto.ValidationIssue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

//    @Scheduled(fixedDelay = 60_000)
    public void validateHotAccounts() {
        List<ValidationIssue> issues = reconciliationService.validateAllAccounts();
        for (ValidationIssue issue : issues) {
            log.warn(
                    "RECONCILIATION_ISSUE severity={} code={} message={}",
                    issue.severity(),
                    issue.code(),
                    issue.message()
            );
        }
    }
}

/*
 * 檔案用途：應用層排程任務，定期驅動快照、資金費、對帳或 Polymarket 同步。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

//    @Scheduled(fixedDelay = 60_000)
    public void validateHotAccounts() {
        for (long uid : List.of(1L, 2L)) {
            reconciliationService.validateUid(uid);
        }
    }
}

package com.lumix.withdrawal.reconciliation;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 鏈上確認 worker 多 instance 以 locked rows 分工；ledger settlement 本身另有 idempotency key。 */
@Component @ConditionalOnBean(WithdrawalChainReconciliationService.class)
public class WithdrawalChainReconciliationWorker {
    private final JdbcTemplate jdbc; private final WithdrawalChainReconciliationService service;
    public WithdrawalChainReconciliationWorker(JdbcTemplate jdbc,WithdrawalChainReconciliationService service){this.jdbc=Objects.requireNonNull(jdbc);this.service=Objects.requireNonNull(service);}
    @Scheduled(fixedDelayString="${LUMIX_WITHDRAWAL_RECONCILIATION_POLL_DELAY:PT10S}") public void process(){claim().forEach(service::reconcile);}
    @Transactional List<String> claim(){return jdbc.query("SELECT withdrawal_id FROM withdrawals WHERE status IN ('BROADCASTED','CONFIRMING') ORDER BY updated_at FOR UPDATE SKIP LOCKED LIMIT 20",(rs,n)->rs.getString(1));}
}

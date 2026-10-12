package com.lumix.withdrawal.signing;

import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 多 pod 以 SKIP LOCKED 拿取 SIGNING 工作；actual service 會再次鎖定確認。 */
@Component
@ConditionalOnBean(WithdrawalSigningExecutionService.class)
public class WithdrawalSigningExecutionWorker {
    private final JdbcTemplate jdbc; private final WithdrawalSigningExecutionService service;
    public WithdrawalSigningExecutionWorker(JdbcTemplate jdbc,WithdrawalSigningExecutionService service){this.jdbc=Objects.requireNonNull(jdbc);this.service=Objects.requireNonNull(service);}
    @Scheduled(fixedDelayString="${LUMIX_WITHDRAWAL_SIGNING_POLL_DELAY:PT5S}") public void process(){claim().forEach(service::sign);}
    @Transactional List<String> claim(){return jdbc.query("SELECT withdrawal_id FROM withdrawals WHERE status='SIGNING' ORDER BY updated_at FOR UPDATE SKIP LOCKED LIMIT 20",(rs,n)->rs.getString(1));}
}

package com.lumix.withdrawal.broadcast;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** SIGNED 與不確定廣播都只會重送已持久化的同一 raw transaction，永不重新簽章。 */
@Component @ConditionalOnBean(WithdrawalBroadcastService.class)
public class WithdrawalBroadcastWorker {
    private final JdbcTemplate jdbc; private final WithdrawalBroadcastService service;
    public WithdrawalBroadcastWorker(JdbcTemplate jdbc,WithdrawalBroadcastService service){this.jdbc=Objects.requireNonNull(jdbc);this.service=Objects.requireNonNull(service);}
    @Scheduled(fixedDelayString="${LUMIX_WITHDRAWAL_BROADCAST_POLL_DELAY:PT5S}") public void process(){claim().forEach(service::broadcast);}
    @Transactional List<String> claim(){return jdbc.query("SELECT withdrawal_id FROM withdrawals WHERE status IN ('SIGNED','BROADCASTING','BROADCAST_UNKNOWN') ORDER BY updated_at FOR UPDATE SKIP LOCKED LIMIT 20",(rs,n)->rs.getString(1));}
}

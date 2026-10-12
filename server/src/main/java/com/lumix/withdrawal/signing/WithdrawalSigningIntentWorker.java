package com.lumix.withdrawal.signing;

import com.lumix.withdrawal.custody.WithdrawalExecutionProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 以資料庫 claim 選取待簽提款，避免多 pod 同時建立多份 intent。
 *
 * <p>execution 開關預設關閉；未經明確啟用時 worker 不會讓任何提款進入簽章階段。</p>
 */
@Component
public class WithdrawalSigningIntentWorker {
    private final JdbcTemplate jdbc; private final WithdrawalSigningIntentService intents; private final WithdrawalExecutionProperties execution;
    public WithdrawalSigningIntentWorker(JdbcTemplate jdbc,WithdrawalSigningIntentService intents,WithdrawalExecutionProperties execution){this.jdbc=Objects.requireNonNull(jdbc);this.intents=Objects.requireNonNull(intents);this.execution=Objects.requireNonNull(execution);}
    @Scheduled(fixedDelayString="${LUMIX_WITHDRAWAL_SIGNING_POLL_DELAY:PT5S}")
    public void process(){ if(!execution.isEnabled()) return; claimBatch().forEach(id->intents.create(id)); }
    @Transactional
    List<String> claimBatch(){ return jdbc.query("SELECT withdrawal_id FROM withdrawals WHERE status='SIGN_PENDING' ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 20",(rs,n)->rs.getString(1)); }
}

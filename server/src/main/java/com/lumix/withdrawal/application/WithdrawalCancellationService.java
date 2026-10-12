package com.lumix.withdrawal.application;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.reservation.ReservationReleaseService;
import com.lumix.withdrawal.domain.WithdrawalStateMachine;
import com.lumix.withdrawal.domain.WithdrawalStatus;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 使用者取消只有在未進 signer/broadcast boundary 前可用，release 與狀態轉換必須同 transaction。 */
@Service
public class WithdrawalCancellationService {
    private final JdbcTemplate jdbc; private final ReservationReleaseService releases; private final WithdrawalStateMachine states=new WithdrawalStateMachine();
    public WithdrawalCancellationService(JdbcTemplate jdbc,ReservationReleaseService releases){this.jdbc=Objects.requireNonNull(jdbc);this.releases=Objects.requireNonNull(releases);}
    @Transactional public boolean cancel(String userId,String withdrawalId) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT withdrawal_id,user_id,request_id,reservation_id,status FROM withdrawals WHERE withdrawal_id=? FOR UPDATE",withdrawalId); if(rows.size()!=1||!userId.equals(rows.getFirst().get("user_id")))throw new ApiException(ApiErrorCode.NOT_FOUND); Map<String,Object> row=rows.getFirst(); WithdrawalStatus current=WithdrawalStatus.valueOf((String)row.get("status")); if(current==WithdrawalStatus.CANCELLED)return true;
        if(!List.of(WithdrawalStatus.REQUESTED,WithdrawalStatus.FUNDS_HELD,WithdrawalStatus.RISK_PENDING,WithdrawalStatus.MANUAL_REVIEW,WithdrawalStatus.RISK_HOLD,WithdrawalStatus.APPROVED,WithdrawalStatus.FINAL_RISK_CHECK,WithdrawalStatus.SIGN_PENDING).contains(current))throw new ApiException(ApiErrorCode.CONFLICT);
        states.requireTransition(current,WithdrawalStatus.CANCELLED); if(jdbc.update("UPDATE withdrawals SET status='CANCELLED',updated_at=CURRENT_TIMESTAMP WHERE withdrawal_id=? AND status=?",withdrawalId,current.name())!=1)throw new IllegalStateException("withdrawal cancel state race");
        if(row.get("reservation_id")!=null)releases.release((String)row.get("reservation_id"),(String)row.get("request_id"),"withdrawal-cancel:"+withdrawalId,userId);
        jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,request_id,outcome,after_state) VALUES ('USER',?,'WITHDRAWAL_CANCELLED','WITHDRAWAL',? ,?,'SUCCESS','CANCELLED')",userId,withdrawalId,row.get("request_id")); return true;
    }
}

package com.lumix.withdrawal.address;

import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 使用者地址簿的 persistence runtime；刪除一律轉為 DISABLED 以保留提款可稽核歷史。 */
@Service
public class WithdrawalAddressBookService {
    private final JdbcTemplate jdbc; private final WithdrawalPlatformProperties properties; private final Clock clock;
    public WithdrawalAddressBookService(JdbcTemplate jdbc, WithdrawalPlatformProperties properties) { this(jdbc, properties, Clock.systemUTC()); }
    WithdrawalAddressBookService(JdbcTemplate jdbc, WithdrawalPlatformProperties properties, Clock clock) { this.jdbc=Objects.requireNonNull(jdbc); this.properties=Objects.requireNonNull(properties); this.clock=Objects.requireNonNull(clock); }
    @Transactional
    public Address create(String userId, String asset, String address, String label, String ip, UUID deviceId) {
        String normalized=EthereumWithdrawalAddress.normalize(address); Instant now=Instant.now(clock); UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO withdrawal_addresses(id,user_id,network,asset_symbol,address,normalized_address,label,status,verified_at,cooldown_until,created_ip,created_device_id) VALUES (?,?, 'ETHEREUM', ?,?,?,?,'COOLDOWN',?,?,?,?)", id,userId,asset.toUpperCase(),address.trim(),normalized,label,now,now.plus(properties.getAddressCooldown()),ip,deviceId);
        audit(userId,"WITHDRAWAL_ADDRESS_CREATED",id.toString(),"COOLDOWN"); return get(userId,id.toString());
    }
    public List<Address> list(String userId) { activateEligible(userId); return jdbc.query("SELECT id,asset_symbol,address,normalized_address,label,status,cooldown_until,created_at FROM withdrawal_addresses WHERE user_id=? ORDER BY created_at DESC",(rs,n)->new Address(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getObject(7,Instant.class),rs.getObject(8,Instant.class)),userId); }
    @Transactional public void disable(String userId,String id) { if(jdbc.update("UPDATE withdrawal_addresses SET status='DISABLED',disabled_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=? AND status <> 'DISABLED'",UUID.fromString(id),userId)!=1) throw new IllegalArgumentException("withdrawal address is unavailable"); audit(userId,"WITHDRAWAL_ADDRESS_DISABLED",id,"DISABLED"); }
    /** 讀取地址時才完成 cooldown activation，不需要 scheduler 也不會將未到期地址放行。 */
    @Transactional public Address requireActive(String userId,String id,String asset) { activateEligible(userId); return getActive(userId,id,asset); }
    private void activateEligible(String userId) { List<String> ids=jdbc.query("UPDATE withdrawal_addresses SET status='ACTIVE',activated_at=CURRENT_TIMESTAMP WHERE user_id=? AND status='COOLDOWN' AND cooldown_until <= CURRENT_TIMESTAMP RETURNING id",(rs,n)->rs.getString(1),userId); ids.forEach(id->audit(userId,"WITHDRAWAL_ADDRESS_ACTIVATED",id,"ACTIVE")); }
    private Address getActive(String userId,String id,String asset) { List<Address> r=jdbc.query("SELECT id,asset_symbol,address,normalized_address,label,status,cooldown_until,created_at FROM withdrawal_addresses WHERE id=? AND user_id=? AND asset_symbol=? AND status='ACTIVE'",(rs,n)->new Address(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getObject(7,Instant.class),rs.getObject(8,Instant.class)),UUID.fromString(id),userId,asset); if(r.size()!=1) throw new IllegalArgumentException("withdrawal address is not active"); return r.getFirst(); }
    private Address get(String userId,String id) { return jdbc.query("SELECT id,asset_symbol,address,normalized_address,label,status,cooldown_until,created_at FROM withdrawal_addresses WHERE id=? AND user_id=?",(rs,n)->new Address(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getObject(7,Instant.class),rs.getObject(8,Instant.class)),UUID.fromString(id),userId).stream().findFirst().orElseThrow(); }
    private void audit(String actor,String action,String target,String after) { jdbc.update("INSERT INTO audit_logs(actor_type,actor_id,action_type,target_type,target_id,outcome,after_state) VALUES ('USER',?,'"+action+"','WITHDRAWAL_ADDRESS',?,'SUCCESS',?)",actor,target,after); }
    public record Address(String id,String asset,String address,String normalizedAddress,String label,String status,Instant cooldownUntil,Instant createdAt) { }
}

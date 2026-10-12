package com.lumix.withdrawal.api;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.DeviceSecret;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.withdrawal.application.WithdrawalOrchestrator;
import com.lumix.withdrawal.application.WithdrawalCancellationService;
import com.lumix.withdrawal.application.WithdrawalAuthorizationPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 已認證使用者提款 API；owner 永遠來自 session principal，不能由 JSON 指定。 */
@RestController @Profile("infrastructure") @RequestMapping("/api/v1/withdrawals")
public class WithdrawalController {
    private final WithdrawalOrchestrator orchestrator; private final WithdrawalCancellationService cancellations; private final WithdrawalAuthorizationPolicy authorization; private final JdbcTemplate jdbc; private final UserAuthenticationProperties authProperties;
    public WithdrawalController(WithdrawalOrchestrator orchestrator,WithdrawalCancellationService cancellations,WithdrawalAuthorizationPolicy authorization,JdbcTemplate jdbc,UserAuthenticationProperties authProperties) { this.orchestrator=Objects.requireNonNull(orchestrator);this.cancellations=Objects.requireNonNull(cancellations);this.authorization=Objects.requireNonNull(authorization);this.jdbc=Objects.requireNonNull(jdbc);this.authProperties=Objects.requireNonNull(authProperties); }
    /** 建立後只回傳受控處理狀態，絕不將已建立誤稱為鏈上成功。 */
    @PostMapping public ResponseEntity<Response> create(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_SESSION_ID_ATTRIBUTE) UUID sessionId,@RequestHeader(value="Idempotency-Key",required=false) String headerKey,@RequestBody CreateRequest body,HttpServletRequest request) {
        if(body==null || !"ETHEREUM".equalsIgnoreCase(body.network())) throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        String key=idempotencyKey(headerKey,body.idempotencyKey());
        authorization.requireAuthorized(user.userId(),sessionId);
        WithdrawalOrchestrator.Result result=orchestrator.create(new WithdrawalOrchestrator.Command(user.userId(),body.asset().trim().toUpperCase(Locale.ROOT),body.amount(),body.destinationAddressId(),key,"withdrawal-"+UUID.randomUUID(),request.getRemoteAddr(),deviceId(request)));
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL,"no-store").body(Response.from(result));
    }
    /** 僅依 principal 查詢，避免猜測 withdrawal id 造成 IDOR。 */
    @GetMapping public List<Response> list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user) { return jdbc.query("SELECT withdrawal_id,status,amount,asset_symbol,address,created_at FROM withdrawals WHERE user_id=? ORDER BY created_at DESC",(rs,n)->new Response(rs.getString(1),rs.getString(2),rs.getBigDecimal(3).toPlainString(),rs.getString(4),rs.getString(5),rs.getObject(6,Instant.class),false),user.userId()); }
    @GetMapping("/{id}") public Response one(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,@PathVariable String id) { return jdbc.query("SELECT withdrawal_id,status,amount,asset_symbol,address,created_at FROM withdrawals WHERE withdrawal_id=? AND user_id=?",(rs,n)->new Response(rs.getString(1),rs.getString(2),rs.getBigDecimal(3).toPlainString(),rs.getString(4),rs.getString(5),rs.getObject(6,Instant.class),false),id,user.userId()).stream().findFirst().orElseThrow(()->new ApiException(ApiErrorCode.NOT_FOUND)); }
    @PostMapping("/{id}/cancel") public ResponseEntity<Void> cancel(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,@PathVariable String id) { cancellations.cancel(user.userId(),id); return ResponseEntity.noContent().build(); }
    public record CreateRequest(String asset,String network,BigDecimal amount,String destinationAddressId,String idempotencyKey) { }
    public record Response(String withdrawalId,String status,String amount,String asset,String destination,Instant createdAt,boolean replayed) { static Response from(WithdrawalOrchestrator.Result r) { return new Response(r.withdrawalId(),r.status().name(),r.amount().toPlainString(),r.asset(),r.destination(),r.createdAt(),r.replayed()); } }
    /** 損毀或不存在的 device cookie 只能形成未知裝置訊號，不能自行猜測或相信 client 傳來的 ID。 */
    private UUID deviceId(HttpServletRequest request) {
        Cookie[] cookies=request.getCookies(); if(cookies==null)return null;
        for(Cookie cookie:cookies) if(authProperties.getDeviceCookieName().equals(cookie.getName())) try { DeviceSecret device=UserAuthenticationService.parseDeviceCookieValue(cookie.getValue()); return device.deviceId(); } catch(RuntimeException ignored) { return null; }
        return null;
    }
    /** body/header 雙通道只為相容既有 HTTP retry client；兩者不一致不能選擇其中一個默許繼續。 */
    private static String idempotencyKey(String header,String body) {
        String normalizedHeader=header==null?null:header.trim(); String normalizedBody=body==null?null:body.trim();
        if(normalizedHeader!=null&&normalizedHeader.isEmpty())normalizedHeader=null;
        if(normalizedBody!=null&&normalizedBody.isEmpty())normalizedBody=null;
        if(normalizedHeader==null&&normalizedBody==null)throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        if(normalizedHeader!=null&&normalizedBody!=null&&!normalizedHeader.equals(normalizedBody))throw new ApiException(ApiErrorCode.IDEMPOTENCY_CONFLICT);
        String value=normalizedHeader!=null?normalizedHeader:normalizedBody;
        if(value.length()>256)throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        return value;
    }
}

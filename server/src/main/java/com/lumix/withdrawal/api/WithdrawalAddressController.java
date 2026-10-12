package com.lumix.withdrawal.api;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.DeviceSecret;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.withdrawal.address.WithdrawalAddressBookService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提款地址簿 API；DELETE 是可稽核的 DISABLED 狀態，不會物理刪除資料。 */
@RestController @Profile("infrastructure") @RequestMapping("/api/v1/withdrawal-addresses")
public class WithdrawalAddressController {
    private final WithdrawalAddressBookService addresses; private final UserAuthenticationProperties authProperties;
    public WithdrawalAddressController(WithdrawalAddressBookService addresses,UserAuthenticationProperties authProperties) { this.addresses=Objects.requireNonNull(addresses);this.authProperties=Objects.requireNonNull(authProperties); }
    @PostMapping public Response create(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,@RequestBody CreateRequest body,HttpServletRequest request) { return Response.from(addresses.create(user.userId(),body.asset(),body.address(),body.label(),request.getRemoteAddr(),deviceId(request))); }
    @GetMapping public List<Response> list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user) { return addresses.list(user.userId()).stream().map(Response::from).toList(); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> disable(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser user,@PathVariable String id) { addresses.disable(user.userId(),id); return ResponseEntity.noContent().build(); }
    public record CreateRequest(String asset,String address,String label) { }
    public record Response(String id,String asset,String address,String label,String status,Instant cooldownUntil,Instant createdAt) { static Response from(WithdrawalAddressBookService.Address a) { return new Response(a.id(),a.asset(),a.address(),a.label(),a.status(),a.cooldownUntil(),a.createdAt()); } }
    /** 地址建立的裝置快照只能從受控 HttpOnly cookie 取 UUID，損毀 cookie 一律當未知。 */
    private java.util.UUID deviceId(HttpServletRequest request) { Cookie[] cookies=request.getCookies(); if(cookies==null)return null; for(Cookie cookie:cookies)if(authProperties.getDeviceCookieName().equals(cookie.getName()))try{DeviceSecret device=UserAuthenticationService.parseDeviceCookieValue(cookie.getValue());return device.deviceId();}catch(RuntimeException ignored){return null;} return null; }
}

package com.lumix.admin.asset;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 現貨幣種配置的管理端 HTTP 邊界；所有 mutation 都委由 service 重做最高管理員授權與稽核。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/assets/spot-configurations")
public class AdminSpotAssetConfigurationController {
    private final AdminSpotAssetConfigurationService service;

    public AdminSpotAssetConfigurationController(AdminSpotAssetConfigurationService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @GetMapping
    public ResponseEntity<List<Response>> list(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(service.list(actor).stream().map(Response::from).toList());
    }

    @PostMapping
    public ResponseEntity<Response> create(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
                                           @RequestBody CreateRequest request) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Response.from(service.create(actor, request.assetSymbol(), request.internalName(), request.precisionScale())));
    }

    @PatchMapping("/{assetSymbol}/status")
    public ResponseEntity<Response> updateStatus(@RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
                                                 @PathVariable String assetSymbol, @RequestBody StatusRequest request) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Response.from(service.updateStatus(actor, assetSymbol, request.status())));
    }

    public record CreateRequest(String assetSymbol, String internalName, int precisionScale) { }
    public record StatusRequest(String status) { }
    public record Response(String assetSymbol, String internalName, int precisionScale, String status, Instant updatedAt) {
        static Response from(AdminSpotAssetConfiguration value) {
            return new Response(value.assetSymbol(), value.internalName(), value.precisionScale(), value.status(), value.updatedAt());
        }
    }
}

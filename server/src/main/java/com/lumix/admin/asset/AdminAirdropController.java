package com.lumix.admin.asset;

import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端空投 HTTP boundary；只接受已登入的 admin cookie，實際授權與入帳均由 service 再次驗證。 */
@RestController
@Profile("infrastructure")
@RequestMapping("/api/admin/v1/assets/airdrops")
public class AdminAirdropController {
    private final AdminAirdropService service;
    private final AdminAirdropAssetConfigurationService assetConfigurationService;

    public AdminAirdropController(AdminAirdropService service, AdminAirdropAssetConfigurationService assetConfigurationService) {
        this.service = Objects.requireNonNull(service, "service must not be null");
        this.assetConfigurationService = Objects.requireNonNull(assetConfigurationService, "assetConfigurationService must not be null");
    }

    /** 空投幣別永遠由後端現貨幣種設定提供；前端不得自行宣告可入帳的資產。 */
    @GetMapping("/configuration")
    public ResponseEntity<ConfigurationResponse> configuration(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor
    ) {
        List<AssetOption> assets = assetConfigurationService.listActiveSpotAssets(actor).stream()
                .map(value -> new AssetOption(value.assetSymbol(), value.internalName(), value.precisionScale()))
                .toList();
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(new ConfigurationResponse(assets));
    }

    @PostMapping
    public ResponseEntity<Response> grant(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            @RequestBody Request request
    ) {
        AdminAirdropResult result = service.grant(actor, new AdminAirdropCommand(
                request.targetUserId(), request.assetSymbol(), request.amount(), request.activityId(), request.reason()
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new Response(Long.toString(result.ledgerJournalId()), result.replayed()));
    }


    /** 金額一律用 decimal 字串傳輸，禁止 browser number 造成 binary floating-point 金額誤差。 */
    public record Request(String targetUserId, String assetSymbol, BigDecimal amount,
                          String activityId, String reason) { }
    public record Response(String ledgerJournalId, boolean replayed) { }
    public record ConfigurationResponse(List<AssetOption> assets) { }
    public record AssetOption(String assetSymbol, String internalName, int precisionScale) { }
    public record ErrorResponse(String code, String message) { }

    /** 將空投輸入拒絕以穩定 domain code 傳回管理端，避免前端只能看到籠統 400。 */
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleDomainValidation(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "asset adjustment validation failed" : exception.getMessage();
        String code = switch (message) {
            case "amount must not be zero", "airdrop amount must be positive" -> "AIRDROP_AMOUNT_INVALID";
            default -> "ASSET_ADJUSTMENT_INVALID";
        };
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(code, message));
    }
}

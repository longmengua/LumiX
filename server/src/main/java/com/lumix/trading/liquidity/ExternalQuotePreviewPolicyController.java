package com.lumix.trading.liquidity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.admin.superadmin.SuperAdminAccessService;
import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端可提交報價預覽政策；前台只能讀取公開預覽值，不接受瀏覽器提供費率。 */
@RestController
@Profile("infrastructure")
@RequestMapping
public class ExternalQuotePreviewPolicyController {
    private final ExternalQuotePreviewPolicyService service;
    private final SuperAdminAccessService access;
    private final String origin;
    private final ObjectMapper strictJson;

    public ExternalQuotePreviewPolicyController(ExternalQuotePreviewPolicyService service,
                                                SuperAdminAccessService access, ObjectMapper json,
                                                @Value("${lumix.trading.admin-allowed-origin:}") String origin) {
        this.service = service;
        this.access = access;
        this.origin = origin;
        this.strictJson = json.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /** 未配置時回 404，前台不得自行套用示意費率。 */
    @GetMapping("/api/v1/market-data/external-quote-preview-policy")
    public ResponseEntity<ExternalQuotePreviewPolicyService.Configuration> publicCurrent() {
        return service.current().map(value -> ResponseEntity.ok().header("Cache-Control", "no-store").body(value))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 管理端讀取也要檢查最高管理員權限，不依靠頁面是否可見。 */
    @GetMapping("/api/admin/v1/trading/external-quotes/current")
    public ResponseEntity<ExternalQuotePreviewPolicyService.Configuration> adminCurrent(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor) {
        access.requireActiveSuperAdmin(actor);
        return publicCurrent();
    }

    /** 後台同源意圖與權限雙重檢查；保存政策不會啟用 Binance 下單。 */
    @PostMapping(value = "/api/admin/v1/trading/external-quotes", consumes = "application/json")
    public ResponseEntity<ExternalQuotePreviewPolicyService.Configuration> publish(
            @RequestAttribute(ApiAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE) AuthenticatedUser actor,
            HttpServletRequest request) throws IOException {
        if (origin.isBlank() || !origin.equals(request.getHeader("Origin"))
                || !"trading-policy".equals(request.getHeader("X-LumiX-Intent"))
                || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        byte[] body = request.getInputStream().readNBytes(8193);
        if (body.length > 8192) throw new IllegalArgumentException("quote preview policy is too large");
        var policy = strictJson.readValue(body, ExternalQuotePreviewPolicyService.Configuration.class);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.publish(actor, policy));
    }
}

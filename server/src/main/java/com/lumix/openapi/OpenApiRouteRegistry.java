package com.lumix.openapi;

import java.util.List;
import java.util.Set;

/**
 * Open API route metadata registry。
 * 只列出預期 API 契約，不代表 controller 或 handler 已存在。
 */
public class OpenApiRouteRegistry {

    private final List<OpenApiRoute> routes = List.of(
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/time",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public server time metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/symbols",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public symbol metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/depth",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public order-book metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/trades",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public recent-trades metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/ticker",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public ticker metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/kline",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public kline metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/mark-price",
                    false,
                    false,
                    false,
                    Set.of(),
                    ApiRateLimitTier.RETAIL,
                    "Public mark-price metadata."
            ),
            new OpenApiRoute(
                    "GET",
                    "/open-api/v1/account/summary",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.READ),
                    ApiRateLimitTier.RETAIL,
                    "Private account-summary metadata only."
            ),
            new OpenApiRoute(
                    "POST",
                    "/open-api/v1/spot/orders",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.SPOT_TRADE),
                    ApiRateLimitTier.RETAIL,
                    "Private spot-order metadata only."
            ),
            new OpenApiRoute(
                    "DELETE",
                    "/open-api/v1/spot/orders/{orderId}",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.SPOT_TRADE),
                    ApiRateLimitTier.RETAIL,
                    "Private spot-cancel metadata only."
            ),
            new OpenApiRoute(
                    "POST",
                    "/open-api/v1/futures/orders",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.FUTURES_TRADE),
                    ApiRateLimitTier.VIP,
                    "Metadata only. Futures execution stays out of Phase 10."
            ),
            new OpenApiRoute(
                    "POST",
                    "/open-api/v1/margin/borrow",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.MARGIN_TRADE),
                    ApiRateLimitTier.VIP,
                    "Metadata only. Margin execution stays out of Phase 10."
            ),
            new OpenApiRoute(
                    "POST",
                    "/open-api/v1/api-keys",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.READ),
                    ApiRateLimitTier.RETAIL,
                    "Private API-key creation metadata."
            ),
            new OpenApiRoute(
                    "POST",
                    "/open-api/v1/wallet/withdraw",
                    true,
                    true,
                    true,
                    Set.of(ApiKeyPermission.WITHDRAW),
                    ApiRateLimitTier.VIP,
                    "Metadata only. Withdraw permission is disabled by default and requires manual review."
            )
    );

    public List<OpenApiRoute> listRoutes() {
        return routes;
    }

    public List<OpenApiRoute> listPublicRoutes() {
        return routes.stream().filter(route -> !route.privateRoute()).toList();
    }

    public List<OpenApiRoute> listPrivateRoutes() {
        return routes.stream().filter(OpenApiRoute::privateRoute).toList();
    }
}

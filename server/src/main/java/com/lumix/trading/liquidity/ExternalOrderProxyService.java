package com.lumix.trading.liquidity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** 依 source 分派 provider；新增交易所只需新增 router bean，不修改客戶交易流程。 */
@Service
public class ExternalOrderProxyService implements ExternalOrderProxy {
    private final Map<String, ExternalOrderRouter> routers;
    private final ExternalOrderProxySettingsService settings;

    public ExternalOrderProxyService(List<ExternalOrderRouter> providers,
                                     ExternalOrderProxySettingsService settings) {
        this.settings = settings;
        var bySource = new java.util.HashMap<String, ExternalOrderRouter>();
        for (var provider : providers) {
            String source = provider.source();
            if (source == null || !source.matches("[A-Z][A-Z0-9_-]{0,31}")
                    || bySource.putIfAbsent(source, provider) != null)
                throw new IllegalStateException("external order provider source is invalid or duplicated");
        }
        routers = Map.copyOf(bySource);
    }

    /** 送單交由 source 對應 provider；未知來源 fail closed。 */
    @Override public ExternalOrderRouter.Outcome submit(ExternalOrderRouter.Intent intent) {
        Objects.requireNonNull(intent, "external order intent is required");
        return router(intent.source()).submit(intent);
    }

    /** 查詢原 clientOrderId，讓 caller 可在延遲後確認狀態。 */
    @Override public ExternalOrderRouter.Outcome lookup(String source, String clientOrderId) {
        return router(source).lookup(source, clientOrderId);
    }

    /** 回報已載入的 provider 名稱，不包含憑證或帳戶資料。 */
    @Override public List<String> sources() { return routers.keySet().stream().sorted().toList(); }

    private ExternalOrderRouter router(String source) {
        if ("DEMO".equals(source)) settings.requireDemoEnabled();
        var router = routers.get(source);
        if (router == null) throw new IllegalArgumentException("external order source is unavailable");
        return router;
    }
}

package com.lumix.trading.liquidity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** 依 source 分派 provider；新增交易所只需新增 router bean，不修改客戶交易流程。 */
@Service
public class ExternalOrderProxyService implements ExternalOrderProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalOrderProxyService.class);
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
        try (var source = MDC.putCloseable("external.source", contextValue(intent.source()));
             var clientOrder = MDC.putCloseable("client.order.id", contextValue(intent.clientOrderId()));
             var market = MDC.putCloseable("market.symbol", contextValue(intent.marketSymbol()))) {
            try {
                var outcome = router(intent.source()).submit(intent);
                try (var status = MDC.putCloseable("external.status_code", Integer.toString(outcome.statusCode()));
                     var reason = MDC.putCloseable("external.reason_code", Integer.toString(outcome.reasonCode()))) {
                    LOGGER.info("External order submission completed");
                }
                return outcome;
            } catch (RuntimeException exception) {
                LOGGER.warn("External order submission failed: exceptionType={}", exception.getClass().getSimpleName());
                throw exception;
            }
        }
    }

    /** 查詢原 clientOrderId，讓 caller 可在延遲後確認狀態。 */
    @Override public ExternalOrderRouter.Outcome lookup(String source, String clientOrderId) {
        try (var sourceContext = MDC.putCloseable("external.source", contextValue(source));
             var clientOrder = MDC.putCloseable("client.order.id", contextValue(clientOrderId))) {
            try {
                var outcome = router(source).lookup(source, clientOrderId);
                try (var status = MDC.putCloseable("external.status_code", Integer.toString(outcome.statusCode()));
                     var reason = MDC.putCloseable("external.reason_code", Integer.toString(outcome.reasonCode()))) {
                    LOGGER.info("External order lookup completed");
                }
                return outcome;
            } catch (RuntimeException exception) {
                LOGGER.warn("External order lookup failed: exceptionType={}", exception.getClass().getSimpleName());
                throw exception;
            }
        }
    }

    /** 回報已載入的 provider 名稱，不包含憑證或帳戶資料。 */
    @Override public List<String> sources() { return routers.keySet().stream().sorted().toList(); }

    private ExternalOrderRouter router(String source) {
        if ("DEMO".equals(source)) settings.requireDemoEnabled();
        var router = routers.get(source);
        if (router == null) throw new IllegalArgumentException("external order source is unavailable");
        return router;
    }

    /** MDC 不接受 null；缺值以固定文字呈現，且不把未驗證內容拼入 message。 */
    private static String contextValue(String value) { return value == null ? "missing" : value; }
}

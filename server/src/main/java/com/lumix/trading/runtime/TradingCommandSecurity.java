package com.lumix.trading.runtime;

import com.lumix.api.error.ApiErrorCode;
import com.lumix.api.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Session cookie 的交易命令另驗證明確來源與自訂 header，阻擋跨站表單及 cookie CSRF。 */
@Component
public class TradingCommandSecurity {
    private final String origin;
    private final boolean enabled;
    public TradingCommandSecurity(@Value("${lumix.trading.allowed-origin:}") String origin,
                                  @Value("${lumix.trading.spot-enabled:false}") boolean enabled) {
        this.origin=origin;this.enabled=enabled;
        if(!origin.isBlank()) {
            URI uri=URI.create(origin);
            if(!("https".equals(uri.getScheme())||"http".equals(uri.getScheme()))||uri.getHost()==null
                    ||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null
                    ||(uri.getRawPath()!=null&&!uri.getRawPath().isEmpty()))throw new IllegalArgumentException("trading origin must be an exact origin");
        }
    }

    /** 停新單仍允許安全撤單；缺少來源設定時不接受任何資金命令。 */
    public void require(HttpServletRequest request,boolean opening) {
        if(origin.isBlank()||!origin.equals(request.getHeader("Origin"))
                ||!"trading-command".equals(request.getHeader("X-LumiX-Intent"))
                ||"cross-site".equals(request.getHeader("Sec-Fetch-Site")))throw new ApiException(ApiErrorCode.AUTHORIZATION_ERROR);
        if(opening&&!enabled)throw new ApiException(ApiErrorCode.SERVICE_UNAVAILABLE);
    }
}

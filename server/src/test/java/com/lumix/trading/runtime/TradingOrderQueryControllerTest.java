package com.lumix.trading.runtime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.SessionSecret;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** MockMvc 驗證既有全域認證 filter 與新只讀 API 的公開邊界；SQL owner 隔離另以 PostgreSQL 驗證。 */
class TradingOrderQueryControllerTest {
    private TradingOrderQueryService queries;
    private MockMvc mvc;
    private Cookie cookie;

    @BeforeEach
    void buildWithExistingAuthenticationFilter() {
        queries = mock(TradingOrderQueryService.class);
        UserAuthenticationService authentication = mock(UserAuthenticationService.class);
        when(authentication.authenticate(any(SessionSecret.class))).thenReturn(new AuthenticatedUser("owner", "owner@example.test", "Owner"));
        cookie = new Cookie("LUMIX_SESSION", UUID.randomUUID() + ".test-only-session-secret");
        mvc = MockMvcBuilders.standaloneSetup(new TradingOrderQueryController(queries))
                .setControllerAdvice(new TradingOrderQueryExceptionHandler())
                .addFilters(new ApiAuthenticationFilter(authentication, new UserAuthenticationProperties(), new ObjectMapper()))
                .build();
    }

    /** 未登入必須在查詢 DB 前拒絕，不將缺 principal 變成可匿名讀取訂單的路徑。 */
    @Test
    void unauthenticatedRequestNeverReachesOrderQueries() throws Exception {
        mvc.perform(get("/api/v1/trading/spot/orders")).andExpect(status().isUnauthorized());
        verifyNoInteractions(queries);
    }

    /** Browser 額外傳 userId 也不能替換 session owner；金額以 decimal string 回傳且禁止快取。 */
    @Test
    void authenticatedReadUsesSessionOwnerAndExactDecimalStrings() throws Exception {
        var order = new TradingOrderQueryService.OrderView("order-1", "BTC-USDT", "BUY", "LIMIT", "GTC", "OPEN",
                "60000.01", "0.0001", "0", "0.0001", Instant.parse("2026-11-21T00:00:00Z"), Instant.parse("2026-11-21T00:00:00Z"));
        when(queries.list("owner", "spot", 20, null)).thenReturn(new TradingOrderQueryService.Page(List.of(order), null));
        mvc.perform(get("/api/v1/trading/spot/orders").cookie(cookie).param("limit", "20").param("userId", "other-owner"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items[0].price").isString()).andExpect(jsonPath("$.items[0].price").value("60000.01"))
                .andExpect(jsonPath("$.items[0].quantity").value("0.0001"));
        verify(queries).list("owner", "spot", 20, null);
    }

    /** 他人／未知訂單及資料庫例外都不洩漏內部訊息；參數型別錯誤維持 400。 */
    @Test
    void failuresAreSanitizedAndTyped() throws Exception {
        when(queries.detail("owner", "futures", "unknown")).thenThrow(new NoSuchElementException("other-owner-sensitive-detail"));
        mvc.perform(get("/api/v1/trading/futures/orders/unknown").cookie(cookie))
                .andExpect(status().isNotFound()).andExpect(content().string(not(containsString("other-owner-sensitive-detail"))));
        mvc.perform(get("/api/v1/trading/spot/orders").cookie(cookie).param("limit", "not-an-integer"))
                .andExpect(status().isBadRequest());
        when(queries.list("owner", "spot", null, null)).thenThrow(new IllegalStateException("sensitive-sql"));
        mvc.perform(get("/api/v1/trading/spot/orders").cookie(cookie)).andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("sensitive-sql"))));
    }
}

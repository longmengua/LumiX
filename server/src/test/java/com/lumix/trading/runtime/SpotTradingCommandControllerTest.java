package com.lumix.trading.runtime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumix.infrastructure.security.ApiAuthenticationFilter;
import com.lumix.user.auth.application.UserAuthenticationService;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.AuthenticatedUser;
import com.lumix.user.auth.domain.SessionSecret;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP 資金命令安全邊界；撮合與扣款守恆另由 PostgreSQL 整合測試覆蓋。 */
class SpotTradingCommandControllerTest {
    private SpotTradingExecutionService service;
    private MockMvc mvc;
    private final Cookie session=new Cookie("LUMIX_SESSION",UUID.randomUUID()+".test-only-session-secret");
    private static final String BODY="{\"marketSymbol\":\"BTC-USDT\",\"side\":\"BUY\",\"orderType\":\"LIMIT\",\"price\":\"100\",\"quantity\":\"0.1\"}";
    @BeforeEach void setup(){service=mock(SpotTradingExecutionService.class);mvc=controller(true);}
    private MockMvc controller(boolean enabled) {
        var authentication=mock(UserAuthenticationService.class);
        when(authentication.authenticate(any(SessionSecret.class))).thenReturn(new AuthenticatedUser("owner","owner@example.test","Owner"));
        return MockMvcBuilders.standaloneSetup(new SpotTradingCommandController(service,new TradingCommandSecurity("https://localhost:8088",enabled),new ObjectMapper()))
                .setControllerAdvice(new TradingOrderQueryExceptionHandler())
                .addFilters(new ApiAuthenticationFilter(authentication,new UserAuthenticationProperties(),new ObjectMapper())).build();
    }
    /** Session owner 與冪等鍵傳給服務；browser 不能夾帶其他帳戶或 userId。 */
    @Test void validCommandUsesAuthenticatedOwner() throws Exception {
        var result=new TradingOrderQueryService.OrderView("order","BTC-USDT","BUY","LIMIT","GTC","OPEN","100","0.1","0","0.1",Instant.now(),Instant.now());
        when(service.place(eq("owner"),any(),eq("intent-1"))).thenReturn(result);
        mvc.perform(post("/api/v1/trading/spot/orders").cookie(session).header("Origin","https://localhost:8088").header("X-LumiX-Intent","trading-command")
                .header("Idempotency-Key","intent-1").contentType("application/json").content(BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
        verify(service).place(eq("owner"),argThat(command->command.price().equals("100")),eq("intent-1"));
    }
    /** Cookie 不能單獨授權跨站資金命令；未登入同樣在服務前拒絕。 */
    @Test void missingSessionAndCrossOriginNeverReachExecution() throws Exception {
        mvc.perform(post("/api/v1/trading/spot/orders").contentType("application/json").content(BODY)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/trading/spot/orders").cookie(session).header("Origin","https://attacker.example").header("X-LumiX-Intent","trading-command")
                .header("Idempotency-Key","intent-1").contentType("application/json").content(BODY)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    /** 拒絕 JSON number、越權欄位及過大 body，避免浮點轉字串與 mass assignment。 */
    @Test void unsafeInputNeverReachesExecution() throws Exception {
        for(String body:new String[]{BODY.replace("\"100\"","100"),BODY.replace("\"100\"","1e2"),BODY.replace("}",",\"userId\":\"victim\"}")," ".repeat(4097)}) {
            mvc.perform(post("/api/v1/trading/spot/orders").cookie(session).header("Origin","https://localhost:8088").header("X-LumiX-Intent","trading-command")
                    .header("Idempotency-Key","intent-1").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    /** 停新單不阻擋撤單；兩條路徑仍需相同來源驗證與 durable key。 */
    @Test void newOrderSwitchDoesNotBlockSafeCancellation() throws Exception {
        mvc=controller(false);
        mvc.perform(post("/api/v1/trading/spot/orders").cookie(session).header("Origin","https://localhost:8088").header("X-LumiX-Intent","trading-command")
                .header("Idempotency-Key","intent-1").contentType("application/json").content(BODY)).andExpect(status().isServiceUnavailable());
        mvc.perform(delete("/api/v1/trading/spot/orders/order").cookie(session).header("Origin","https://localhost:8088").header("X-LumiX-Intent","trading-command")
                .header("Idempotency-Key","cancel-1")).andExpect(status().isOk());
        verify(service).cancel("owner","order","cancel-1");verify(service,never()).place(any(),any(),any());
    }
}

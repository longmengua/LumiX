package com.lumix;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 啟動骨架測試。
 * 只驗證應用程式上下文可以載入，避免 Phase 9 skeleton 缺少主類。
 */
@SpringBootTest
class LumiXServerApplicationTests {

    @Test
    void contextLoads() {
        // 空測試即可，目的是確認 Spring 上下文可以建立。
    }
}

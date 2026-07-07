package com.lumix.integration;

import org.junit.jupiter.api.Test;

/**
 * integration test smoke case。
 *
 * 這個測試只確認 Spring context 可以在 integration profile 下穩定啟動，
 * 不代表任何交易 runtime、錢包或 ledger 流程已完成。
 */
class IntegrationSmokeTest extends IntegrationTestSupport {

    /**
     * 確認 integration smoke test 可以載入 application context。
     */
    @Test
    void contextLoads() {
        // 空測試即可，重點是驗證 integration test foundation 能一致執行。
    }
}

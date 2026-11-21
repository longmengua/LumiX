package com.lumix.trading.runtime;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/** 在沒有 Compose 的開發環境重跑既有現貨資金整合案例，仍限定於隔離的 lumix_test 資料庫。 */
class SpotTradingEmbeddedIntegrationTest extends SpotTradingExecutionIntegrationTest {
    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void startIsolatedPostgres() throws Exception {
        postgres = EmbeddedPostgres.start();
        System.setProperty("lumix.test.postgres.jdbc-url",
                "jdbc:postgresql://127.0.0.1:" + postgres.getPort() + "/lumix_test");
        System.setProperty("lumix.test.postgres.username", "postgres");
        System.setProperty("lumix.test.postgres.password", "embedded-test-only");
    }

    @AfterAll
    static void stopIsolatedPostgres() throws Exception {
        try {
            System.clearProperty("lumix.test.postgres.jdbc-url");
            System.clearProperty("lumix.test.postgres.username");
            System.clearProperty("lumix.test.postgres.password");
        } finally {
            postgres.close();
        }
    }
}

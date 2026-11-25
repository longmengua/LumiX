package com.lumix.trading.core.futures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** 在隔離的真實 PostgreSQL 上驗證合約持倉與風險政策 migration，且證據維持不可變。 */
class FuturesHedgeMigrationTest {
    @Test
    void migrationsCreateHedgePositionsAndImmutableEvents() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            var dataSource = postgres.getPostgresDatabase();
            var applied = Flyway.configure().dataSource(dataSource)
                    .locations("classpath:db/migration").load().migrate();
            var jdbc = new JdbcTemplate(dataSource);
            assertEquals(1, applied.migrationsExecuted);
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_class WHERE relname='futures_hedge_positions'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_class WHERE relname='futures_hedge_position_events'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='immutable_futures_position_events'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_class WHERE relname='futures_risk_policies'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='immutable_futures_risk_policies'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='trading_order_position_boundary'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='immutable_futures_funding_cycles'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='futures_liquidation_order_boundary'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='immutable_futures_liquidation_attempts'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname='trading_instruments_status_check'", Integer.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM pg_trigger WHERE tgname='immutable_external_quote_preview_policies'", Integer.class));
        }
    }
}

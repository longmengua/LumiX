package com.lumix.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.util.Assert;

/**
 * 在 infrastructure profile 啟用資料庫連線池與讀寫路由。
 *
 * <p>Flyway 另以 primary URL 執行 migration；這裡的路由只服務 application runtime。若 replica
 * 未被明確提供或未通過部署端健康檢查，必須使用 SINGLE，而不是讓讀取落到未知資料來源。</p>
 */
@Configuration
@Profile("infrastructure")
public class DatabaseTopologyConfiguration {

    @Bean(name = "lumixWriteDataSource", destroyMethod = "close")
    HikariDataSource writeDataSource(DatabaseTopologyProperties properties) {
        return dataSource("lumix-write", properties.getPrimary());
    }

    @Bean(name = "lumixReadDataSource")
    DataSource readDataSource(
        DatabaseTopologyProperties properties,
        @Qualifier("lumixWriteDataSource") DataSource writeDataSource
    ) {
        if (properties.getMode() == DatabaseTopologyProperties.Mode.SINGLE) {
            return writeDataSource;
        }
        return dataSource("lumix-read", properties.getReplica());
    }

    @Bean(name = "dataSource")
    @Primary
    DataSource dataSource(
        @Qualifier("lumixWriteDataSource") DataSource writeDataSource,
        @Qualifier("lumixReadDataSource") DataSource readDataSource
    ) {
        ReadWriteRoutingDataSource routing = new ReadWriteRoutingDataSource();
        routing.setTargetDataSources(Map.of(
            ReadWriteRoutingDataSource.DatabaseRole.WRITE, writeDataSource,
            ReadWriteRoutingDataSource.DatabaseRole.READ, readDataSource
        ));
        routing.setDefaultTargetDataSource(writeDataSource);
        routing.afterPropertiesSet();
        return routing;
    }

    private HikariDataSource dataSource(String poolName, DatabaseTopologyProperties.Connection connection) {
        Assert.hasText(connection.getJdbcUrl(), poolName + " jdbcUrl is required");
        Assert.hasText(connection.getUsername(), poolName + " username is required");
        Assert.hasText(connection.getPassword(), poolName + " password is required");
        Assert.isTrue(connection.getMaximumPoolSize() > 0, poolName + " maximumPoolSize must be positive");

        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(connection.getJdbcUrl());
        config.setUsername(connection.getUsername());
        config.setPassword(connection.getPassword());
        config.setMaximumPoolSize(connection.getMaximumPoolSize());
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5_000);
        config.setValidationTimeout(3_000);
        return new HikariDataSource(config);
    }
}

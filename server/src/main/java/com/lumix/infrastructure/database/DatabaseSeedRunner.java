package com.lumix.infrastructure.database;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** Applies idempotent initial configuration only when the deployment explicitly enables seeding. */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.database.seed", name = "enabled", havingValue = "true")
public class DatabaseSeedRunner implements ApplicationRunner {
    private final DataSource dataSource;

    public DatabaseSeedRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        new ResourceDatabasePopulator(new ClassPathResource("db/seed.sql")).execute(dataSource);
    }
}

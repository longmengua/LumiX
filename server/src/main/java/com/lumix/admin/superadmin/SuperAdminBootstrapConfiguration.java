package com.lumix.admin.superadmin;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** 將 bootstrap 放在 migration 完成後的 ApplicationRunner，避免先查詢尚未建立的 principal schema。 */
@Configuration
@Profile("infrastructure")
class SuperAdminBootstrapConfiguration {

    @Bean
    ApplicationRunner superAdminBootstrapRunner(SuperAdminBootstrapService service) {
        return arguments -> service.bootstrap();
    }
}

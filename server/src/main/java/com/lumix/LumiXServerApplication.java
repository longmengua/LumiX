package com.lumix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * LumiX Server 的 Spring Boot 啟動入口。
 * Phase 9 僅提供骨架，不包含任何生產級資產邏輯。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LumiXServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LumiXServerApplication.class, args);
    }
}

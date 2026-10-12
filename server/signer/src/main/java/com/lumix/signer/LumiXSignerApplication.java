package com.lumix.signer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 獨立 signer 進程；此模組刻意不依賴 lumix-server 的 DB、session、ledger 或 withdrawal approval。 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LumiXSignerApplication {
    public static void main(String[] args) { SpringApplication.run(LumiXSignerApplication.class, args); }
}

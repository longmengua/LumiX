package com.example.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 專案的主程式入口。
 *
 * - 使用 @SpringBootApplication 標註，代表這是一個 Spring Boot 應用。
 * - 這個類別會觸發自動配置 (auto-configuration)，掃描 com.example 底下的元件。
 * - main 方法是 Java 程式啟動點，會呼叫 SpringApplication.run() 啟動整個應用。
 */
@SpringBootApplication
public class ExchangeApplication {

    /**
     * Java 的 main 函數。
     * SpringApplication.run() 會：
     *   1. 啟動 Spring 容器 (ApplicationContext)
     *   2. 掃描組件、載入配置 (Redis, Kafka 等等)
     *   3. 開啟內建的 Web Server (預設為 Tomcat，這裡也可以切換成 Undertow/Jetty)
     *
     * 執行方式：
     *   mvn spring-boot:run
     * 或者打包後：
     *   java -jar target/futures-margin-snap-0.0.1-SNAPSHOT.jar
     */
    public static void main(String[] args) {
        SpringApplication.run(ExchangeApplication.class, args);
    }
}

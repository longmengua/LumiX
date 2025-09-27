package com.example.java21_OLAP.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis 組態（使用 Lettuce）
 * - 在開發環境：預設連 localhost:6379（你可用 application.yml 覆寫）
 * - 暴露 RedisTemplate<String, Object> 供 Repository 使用
 */
@Configuration
public class RedisConfig {

    /** 連線工廠（可由 yml 指定 host/port/password/db） */
    @Bean
    LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }

    /** 通用 RedisTemplate（Value 直接存放物件；生產建議自訂序列化器） */
    @Bean
    RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory f) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(f);
        // TODO: 設定 key/value/Hash 的序列化策略（例如 Jackson 或 Kryo）
        return t;
    }
}

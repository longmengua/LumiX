package com.example.exchange.infra.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 組態（使用 Jackson JSON 序列化）
 * - 解決 "Cannot serialize"：物件以 JSON 存，避免 JDK 序列化需要 implements Serializable
 * - 支援 Instant/LocalDateTime 等 Java Time 型別
 */
@Configuration
public class RedisConfig {

    @Bean
    LettuceConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory f) {
        // 1) 建立 ObjectMapper：支援 JavaTime、開啟多型處理（可存放各種物件）
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());                 // Instant/LocalDateTime
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 多型啟用（讓 value 是 Object/各種 domain 類別也能還原）
        om.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 2) 建立序列器
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

        // 3) 配置 RedisTemplate
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(f);

        // Key 與 Hash Key 一律用 String
        t.setKeySerializer(keySerializer);
        t.setHashKeySerializer(keySerializer);

        // Value 與 Hash Value 用 JSON
        t.setValueSerializer(jsonSerializer);
        t.setHashValueSerializer(jsonSerializer);

        t.afterPropertiesSet();
        return t;
    }
}

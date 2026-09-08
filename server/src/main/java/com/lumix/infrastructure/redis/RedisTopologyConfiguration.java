package com.lumix.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;

/**
 * Redis connection factory 的 topology adapter。
 *
 * <p>業務程式只依賴 {@link RedisConnectionFactory} / {@link RedisTemplate}，不可把單點或 cluster
 * client 建構散落在 rate-limit、session 或 cache 邏輯，否則將來切換 topology 時容易產生不一致。</p>
 */
@Configuration
@Profile("infrastructure")
public class RedisTopologyConfiguration {

    @Bean(destroyMethod = "destroy")
    LettuceConnectionFactory redisConnectionFactory(RedisTopologyProperties properties) {
        if (properties.getMode() == RedisTopologyProperties.Mode.CLUSTER) {
            Assert.notEmpty(properties.getClusterNodes(), "Redis clusterNodes are required in cluster mode");
            RedisClusterConfiguration cluster = new RedisClusterConfiguration(properties.getClusterNodes());
            applyPassword(cluster, properties.getPassword());
            return new LettuceConnectionFactory(cluster);
        }

        Assert.hasText(properties.getHost(), "Redis host is required in standalone mode");
        Assert.isTrue(properties.getPort() > 0, "Redis port must be positive");
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        applyPassword(standalone, properties.getPassword());
        return new LettuceConnectionFactory(standalone);
    }

    @Bean
    RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        return template;
    }

    private void applyPassword(RedisStandaloneConfiguration configuration, String password) {
        if (password != null && !password.isBlank()) {
            configuration.setPassword(RedisPassword.of(password));
        }
    }

    private void applyPassword(RedisClusterConfiguration configuration, String password) {
        if (password != null && !password.isBlank()) {
            configuration.setPassword(RedisPassword.of(password));
        }
    }
}

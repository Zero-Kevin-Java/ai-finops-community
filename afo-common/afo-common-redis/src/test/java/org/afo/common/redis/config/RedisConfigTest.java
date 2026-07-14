package org.afo.common.redis.config;

import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void normalizeBlankPasswordClearsSingleServerPassword() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setPassword("");
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://127.0.0.1:6379")
            .setPassword("");

        RedisConfig.normalizeBlankPassword(config, redisProperties);

        assertThat(config.useSingleServer().getPassword()).isNull();
    }

    @Test
    void normalizeBlankPasswordKeepsNonBlankPassword() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setPassword("secret");
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://127.0.0.1:6379")
            .setPassword("secret");

        RedisConfig.normalizeBlankPassword(config, redisProperties);

        assertThat(config.useSingleServer().getPassword()).isEqualTo("secret");
    }
}

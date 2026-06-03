package com.campusdoc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisVectorConfig {

    @Bean
    public RedisHostPort redisHostPort(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return new RedisHostPort(host, port);
    }

    public record RedisHostPort(String host, int port) {
    }
}

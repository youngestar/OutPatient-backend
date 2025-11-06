package com.std.cuit.admin.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:redis}")  // 默认使用redis服务名
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private String redisPort;

    @Value("${spring.data.redis.password:258013}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 单节点配置
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(redisDatabase)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(2)
                .setTimeout(5000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }
}
package com.std.cuit.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 后台管理服务启动类
 */
@SpringBootApplication(
        scanBasePackages = {"com.std.cuit"}
)
@EnableDiscoveryClient  // 启用Nacos服务发现
@EnableAsync            // 启用异步处理
@MapperScan("com.std.cuit.service.mapper")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
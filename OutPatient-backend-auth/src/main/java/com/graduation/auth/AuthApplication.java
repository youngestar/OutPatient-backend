package com.graduation.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 认证服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient  // 启用Nacos服务发现
@EnableAsync            // 启用异步处理
@ComponentScan(basePackages = {"com.graduation"}) // 扫描整个项目的组件
@MapperScan("com.graduation.service.mapper") // 添加这行，扫描Mapper接口
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
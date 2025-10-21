package com.graduation.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 后台管理服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient  // 启用Nacos服务发现
@EnableAsync            // 启用异步处理
@ComponentScan(basePackages = {"com.graduation"}) // 扫描整个项目的组件
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
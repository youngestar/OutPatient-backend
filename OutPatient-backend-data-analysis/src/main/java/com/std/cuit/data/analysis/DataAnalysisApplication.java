package com.std.cuit.data.analysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableAsync
@EnableRabbit
@ComponentScan({"com.std.cuit.data.analysis", "com.std.cuit.service", "com.std.cuit.common"})
@MapperScan("com.std.cuit.service.mapper")
public class DataAnalysisApplication {
    public static void main(String[] args) {

        SpringApplication.run(DataAnalysisApplication.class, args);
    }
}
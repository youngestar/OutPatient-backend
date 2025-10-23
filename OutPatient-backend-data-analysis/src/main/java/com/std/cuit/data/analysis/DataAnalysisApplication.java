package com.std.cuit.data.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class DataAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataAnalysisApplication.class, args);
    }
}

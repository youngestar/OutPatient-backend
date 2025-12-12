package com.std.cuit.registration.config;

import com.std.cuit.registration.filter.TokenRefreshFilter;
import jakarta.annotation.Resource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class TokenRefreshFilterConfig {

    @Resource
    private TokenRefreshFilter tokenRefreshFilter;

    @Bean
    public FilterRegistrationBean<TokenRefreshFilter> tokenRefreshFilterRegistration() {
        FilterRegistrationBean<TokenRefreshFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(tokenRefreshFilter);
        registration.addUrlPatterns("/*");
        // 将执行顺序设置为较低优先级，确保 Sa-Token 的过滤器先运行
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        registration.setName("TokenRefreshFilter");
        return registration;
    }
}


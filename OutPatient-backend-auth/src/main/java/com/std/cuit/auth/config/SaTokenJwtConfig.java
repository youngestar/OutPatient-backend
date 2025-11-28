package com.std.cuit.auth.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 Sa-Token 切换为 JWT 模式，确保各服务解析同一令牌格式。
 */
@Configuration
public class SaTokenJwtConfig {

    @Bean
    public StpLogic stpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}

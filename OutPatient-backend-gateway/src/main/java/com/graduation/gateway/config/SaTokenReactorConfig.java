package com.graduation.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.List;

/**
 * Sa-Token 权限认证配置类（Gateway WebFlux版本）
 */
@Slf4j
@Configuration
public class SaTokenReactorConfig {

    // 不需要鉴权的路径
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            // 认证相关
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/email",
            "/api/auth/IsExists",
            "/api/auth/logout",

            // 公开接口
            "/api/public/**",

            // 文档相关
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/doc.html",

            // 健康检查
            "/actuator/health",
            "/actuator/info"
    );

    @Bean
    public SaReactorFilter getSaReactorFilter() {
        log.info("初始化 Sa-Token Reactor 过滤器...");

        return new SaReactorFilter()
                // 拦截所有路径
                .addInclude("/**")
                // 排除路径
                .setExcludeList(EXCLUDE_PATHS)
                // 认证函数
                .setAuth(obj -> {
                    ServerWebExchange exchange = (ServerWebExchange) obj;
                    String path = exchange.getRequest().getPath().value();

                    // 登录认证校验
                    SaRouter.match("/**")
                            .notMatch(EXCLUDE_PATHS)
                            .check(r -> {
                                StpUtil.checkLogin();
                                log.debug("用户认证通过: {}", StpUtil.getLoginId());
                            });

                    // 路由权限校验（根据路径进行角色校验）
                    checkRoutePermission(path);
                })

                // 异常处理函数
                .setError(e -> {
                    log.error("网关认证失败: {}", e.getMessage());
                    return SaResult.error("认证失败: " + e.getMessage()).setCode(401);
                });
    }

    /**
     * 检查路由权限（根据请求路径校验角色权限）
     */
    private void checkRoutePermission(String path) {
        // 管理员路径需要admin角色
        if (path.startsWith("/api/admin/")) {
            StpUtil.checkRole("admin");
        }

        // 医生相关路径需要doctor角色
        if (path.startsWith("/api/doctor/")) {
            StpUtil.checkRole("doctor");
        }

        // 患者相关路径需要patient角色
        if (path.startsWith("/api/patient/")) {
            StpUtil.checkRole("patient");
        }
    }

}
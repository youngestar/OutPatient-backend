package com.graduation.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    // 不需要鉴权的路径
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            // 认证相关
            "/auth/login",
            "/auth/logout",
            "/auth/register",
            "/auth/refresh",

            // 公开接口
            "/public/**",
            "/common/**",

            // 文档相关
            "/doc.html",
            "/webjars/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/v2/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",

            // 静态资源
            "/static/**",
            "/resources/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",

            // 健康检查
            "/actuator/health",
            "/health"
    );

    // 需要特定权限的路径映射
    private static final List<PathRoleMapping> PATH_ROLE_MAPPINGS = Arrays.asList(
            new PathRoleMapping("/admin/**", "admin"),
            new PathRoleMapping("/doctor/**", "doctor"),
            new PathRoleMapping("/patient/**", "patient"),
            new PathRoleMapping("/department/**", "admin"),
            new PathRoleMapping("/clinic/**", "admin"),
            new PathRoleMapping("/schedule/**", Arrays.asList("admin", "doctor"))
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("初始化 Sa-Token 拦截器配置...");

        // 注册 Sa-Token 拦截器，自定义详细规则
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 1. 首先排除不需要鉴权的路径
                    SaRouter
                            .match("/**")
                            .notMatch(EXCLUDE_PATHS)
                            .check(r -> {
                                // 2. 检查登录状态
                                StpUtil.checkLogin();

                                // 3. 根据路径进行角色权限校验
                                checkPathRolePermission();
                            });

                })).addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);

        log.info("Sa-Token 拦截器配置完成，排除路径: {}", EXCLUDE_PATHS);
    }

    /**
     * 检查路径对应的角色权限
     */
    private void checkPathRolePermission() {
        String path = SaHolder.getRequest().getRequestPath();

        for (PathRoleMapping mapping : PATH_ROLE_MAPPINGS) {
            if (pathMatcher(path, mapping.getPathPattern())) {
                if (mapping.getRoles().size() == 1) {
                    // 单个角色要求
                    StpUtil.checkRole(mapping.getRoles().get(0));
                } else {
                    // 多个角色要求（满足其中一个即可）
                    boolean hasRole = mapping.getRoles().stream()
                            .anyMatch(role -> StpUtil.hasRole(role));
                    if (!hasRole) {
                        throw new RuntimeException("无权限访问，需要的角色: " + mapping.getRoles());
                    }
                }
                break;
            }
        }
    }

    /**
     * 简单的路径匹配器
     */
    private boolean pathMatcher(String requestPath, String pattern) {
        if ("/**".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 3);
            return requestPath.startsWith(basePattern);
        }
        return requestPath.equals(pattern);
    }

    /**
     * 路径角色映射内部类
     */
    private static class PathRoleMapping {
        private final String pathPattern;
        private final List<String> roles;

        public PathRoleMapping(String pathPattern, String role) {
            this.pathPattern = pathPattern;
            this.roles = Arrays.asList(role);
        }

        public PathRoleMapping(String pathPattern, List<String> roles) {
            this.pathPattern = pathPattern;
            this.roles = roles;
        }

        public String getPathPattern() {
            return pathPattern;
        }

        public List<String> getRoles() {
            return roles;
        }
    }
}
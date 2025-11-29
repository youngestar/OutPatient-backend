package com.std.cuit.auth.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一将自定义 satoken 请求头写入 Sa-Token 上下文, 确保 StpUtil 能读取到 token。
 */
@Component
@Order(SaTokenConsts.SA_TOKEN_CONTEXT_FILTER_ORDER + 1)
public class SaTokenHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (StringUtils.hasText(token) && !StringUtils.hasText(StpUtil.getTokenValue())) {
            StpUtil.getStpLogic().setTokenValueToStorage(token.trim());
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("satoken");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("sa-token");
        }
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("Sa-Token");
        }
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("sa-token-authorization");
        }
        return token;
    }
}

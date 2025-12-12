package com.std.cuit.registration.filter;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class TokenRefreshFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String path = httpRequest.getRequestURI();

        // 尝试从常见位置读取 token（兼容 header / param / Authorization）
        String token = httpRequest.getHeader("satoken");
        if (token == null || token.isEmpty()) {
            token = httpRequest.getParameter("satoken");
        }
        if ((token == null || token.isEmpty()) && httpRequest.getHeader("Authorization") != null) {
            token = httpRequest.getHeader("Authorization");
            // 如果是 Bearer xxx 的形式，提取实际 token 值（兼容）
            if (token != null && token.toLowerCase().startsWith("bearer ")) {
                token = token.substring(7);
            }
        }

        // 只要请求携带 token（或当前上下文已登录），尝试续期（容错处理）
        try {
            if ((token != null && !token.isEmpty()) || StpUtil.isLogin()) {
                // 如果当前上下文未识别到登录（但请求携带 token），StpUtil.isLogin() 可能为 false，
                // 此处只在识别为登录状态时刷新活跃时间，避免抛出异常影响请求流程。
                if (StpUtil.isLogin()) {
                    StpUtil.updateLastActiveToNow();
                    log.debug("自动续期 token, userId: {}", StpUtil.getLoginId());
                } else {
                    // 如果请求携带 token 但上下文未识别到，尝试将 token 注入 Sa-Token 上下文并重试续期
                    if (token != null && !token.isEmpty()) {
                        try {
                            StpUtil.getStpLogic().setTokenValueToStorage(token.trim());
                            if (StpUtil.isLogin()) {
                                StpUtil.updateLastActiveToNow();
                                log.debug("注入 token 并自动续期成功, userId: {}", StpUtil.getLoginId());
                            } else {
                                log.debug("注入 token 后仍未识别为已登录, path: {}", path);
                            }
                        } catch (Exception ex) {
                            log.warn("注入 token 到 Sa-Token 上下文失败: {}", ex.getMessage());
                        }
                    } else {
                        log.debug("请求未携带 token，且当前上下文未识别为已登录, path: {}", path);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Token续期失败: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
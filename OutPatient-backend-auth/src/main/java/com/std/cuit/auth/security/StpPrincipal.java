package com.std.cuit.auth.security;

import lombok.Getter;

import java.security.Principal;

/**
 * SaToken的Principal实现，用于WebSocket用户身份识别
 * @author hua
 */
@Getter
public class StpPrincipal implements Principal {

    /**
     * -- GETTER --
     *  获取登录ID
     *
     */
    private final String loginId;
    
    public StpPrincipal(String loginId) {
        this.loginId = loginId;
    }
    
    @Override
    public String getName() {
        return this.loginId;
    }

} 
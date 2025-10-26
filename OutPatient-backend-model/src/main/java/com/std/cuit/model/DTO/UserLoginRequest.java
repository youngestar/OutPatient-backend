package com.std.cuit.model.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginRequest {
    /**
     * 登录账号（用户名或邮箱）
     */
    @NotBlank(message = "登录账号不能为空")
    private String account;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 是否记住我（默认为false）
     */
    @Builder.Default
    private Boolean rememberMe = false;
}
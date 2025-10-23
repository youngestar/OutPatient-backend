package com.std.cuit.auth.controller;

import com.std.cuit.model.VO.UserVO;
import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ResultUtils;
import com.std.cuit.model.DTO.UserLoginRequest;
import com.std.cuit.model.DTO.UserRegisterRequest;
import com.std.cuit.model.DTO.UserUpdateRequest;
import com.std.cuit.service.service.AuthService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author hua
 * &#064;description  登录鉴权、用户信息相关接口
 * &#064;create  2025-03-31 21:30
 */

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Api(tags = "登录鉴权、用户信息相关接口")
public class AuthController {

    @Resource
    private final AuthService authService;

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @return 处理结果
     */
    @GetMapping("/email")
    @Operation(summary = "发送邮箱验证码", description = "发送邮箱验证码")
    public BaseResponse<Void> sendEmailCode(@Parameter(description = "邮箱地址") @RequestParam String email) {
        authService.sendEmailCode(email);
        return ResultUtils.success(null);
    }

    /**
     * 检查用户名或邮箱是否已存在
     * @param username 用户名
     * @param email 邮箱
     * @return 是否存在
     */
    @GetMapping("/IsExists")
    @Operation(summary = "检查用户名或邮箱是否已存在", description = "检查用户名或邮箱是否已存在")
    public BaseResponse<Boolean> checkUserExists(
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email) {
        boolean exists = authService.checkUserExists(username, email);
        return ResultUtils.success(exists);
    }

    /**
     * 用户注册
     * @param registerRequest 注册信息
     * @return 处理结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册")
    public BaseResponse<Void> register(@Parameter(description = "注册信息") @RequestBody UserRegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResultUtils.success(null);
    }

    /**
     * 用户登录
     * @param loginRequest 登录信息
     * @return 登录成功的用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录")
    public BaseResponse<UserVO> login(@Parameter(description = "登录信息") @RequestBody UserLoginRequest loginRequest) {
        UserVO userVO = authService.login(loginRequest);
        return ResultUtils.success(userVO);
    }

    /**
     * 退出登录
     * @return 处理结果
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "退出登录")
    public BaseResponse<Void> logout() {
        authService.logout();
        return ResultUtils.success(null);
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/currentUser")
    @Operation(summary = "获取当前登录用户信息", description = "获取当前登录用户信息")
    public BaseResponse<UserVO> getCurrentUserInfo() {
        UserVO userVO = authService.getCurrentUserInfo();
        return ResultUtils.success(userVO);
    }

    /**
     * 修改密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 处理结果
     */
    @PostMapping("/updatePassword")
    @Operation(summary = "修改密码", description = "修改密码")
    public BaseResponse<Void> updatePassword(@Parameter(description = "旧密码") @RequestParam String oldPassword, @RequestParam String newPassword) {
        authService.updatePassword(oldPassword, newPassword);
        return ResultUtils.success(null);
    }

    /**
     * 上传或更新用户头像
     * @param file 头像文件
     * @return 新的头像URL
     */
    @PostMapping("/updateAvatar")
    @Operation(summary = "上传或更新用户头像", description = "上传或更新用户头像")
    public BaseResponse<String> updateAvatar(@Parameter(description = "头像文件") @RequestParam("file") MultipartFile file) {
        String avatarUrl = authService.updateAvatar(file);
        return ResultUtils.success(avatarUrl);
    }

    /**
     * 更新用户个人信息
     * @param updateRequest 用户个人信息
     * @return 更新后的用户信息
     */
    @PostMapping("/updateInfo")
    @Operation(summary = "更新用户个人信息", description = "更新用户个人信息")
    public BaseResponse<UserVO> updateUserInfo(@Parameter(description = "用户个人信息") @RequestBody UserUpdateRequest updateRequest) {
        log.info("接收到更新用户个人信息请求");
        UserVO userVO = authService.updateUserInfo(updateRequest);
        return ResultUtils.success(userVO);
    }
}
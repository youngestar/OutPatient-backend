package com.graduation.auth.controller;

import com.graduation.model.VO.UserVO;
import com.graduation.common.common.BaseResponse;
import com.graduation.common.common.ResultUtils;
import com.graduation.model.DTO.UserLoginRequest;
import com.graduation.model.DTO.UserRegisterRequest;
import com.graduation.model.DTO.UserUpdateRequest;
import com.graduation.service.service.AuthService;
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
public class AuthController {

    @Resource
    private final AuthService authService;

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @return 处理结果
     */
    @GetMapping("/email")
    public BaseResponse<Void> sendEmailCode(@RequestParam String email) {
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
    public BaseResponse<Boolean> checkUserExists(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        boolean exists = authService.checkUserExists(username, email);
        return ResultUtils.success(exists);
    }

    /**
     * 用户注册
     * @param registerRequest 注册信息
     * @return 处理结果
     */
    @PostMapping("/register")
    public BaseResponse<Void> register(@RequestBody UserRegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResultUtils.success(null);
    }

    /**
     * 用户登录
     * @param loginRequest 登录信息
     * @return 登录成功的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> login(@RequestBody UserLoginRequest loginRequest) {
        UserVO userVO = authService.login(loginRequest);
        return ResultUtils.success(userVO);
    }

    /**
     * 退出登录
     * @return 处理结果
     */
    @PostMapping("/logout")
    public BaseResponse<Void> logout() {
        authService.logout();
        return ResultUtils.success(null);
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @GetMapping("/currentUser")
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
    public BaseResponse<Void> updatePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        authService.updatePassword(oldPassword, newPassword);
        return ResultUtils.success(null);
    }

    /**
     * 上传或更新用户头像
     * @param file 头像文件
     * @return 新的头像URL
     */
    @PostMapping("/updateAvatar")
    public BaseResponse<String> updateAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = authService.updateAvatar(file);
        return ResultUtils.success(avatarUrl);
    }

    /**
     * 更新用户个人信息
     * @param updateRequest 用户个人信息
     * @return 更新后的用户信息
     */
    @PostMapping("/updateInfo")
    public BaseResponse<UserVO> updateUserInfo(@RequestBody UserUpdateRequest updateRequest) {
        log.info("接收到更新用户个人信息请求");
        UserVO userVO = authService.updateUserInfo(updateRequest);
        return ResultUtils.success(userVO);
    }
}
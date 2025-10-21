package com.graduation.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.graduation.model.entity.User;

/**
 * <p>
 * 用户基本信息表 服务类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    User getByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户对象
     */
    User getByEmail(String email);
}

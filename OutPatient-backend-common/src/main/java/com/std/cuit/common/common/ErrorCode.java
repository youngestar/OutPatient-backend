package com.std.cuit.common.common;

import lombok.Getter;

/**
 * 自定义错误码
 */
@Getter
public enum ErrorCode {
    // 成功
    SUCCESS(200, "ok"),

    // 通用错误 400-499
    PARAMS_ERROR(400, "请求参数错误"),
    NULL_ERROR(400, "请求数据为空"),
    DATA_EXISTS(409, "数据已存在"), // 409 Conflict 更适合数据已存在的情况
    DATA_NOT_EXISTS(404, "数据不存在"),

    // 认证授权 401-403
    NOT_LOGIN(401, "未登录"),
    NO_AUTH(403, "无权限"), // 403 更适合无权限的情况
    TOKEN_EXPIRED(401, "Token已过期"),
    TOKEN_INVALID(401, "Token无效"),

    // 用户相关 400-409
    USER_NOT_EXIST(404, "用户不存在"),
    USER_EXISTS(409, "用户已存在"),
    PASSWORD_ERROR(401, "密码错误"), // 密码错误属于认证问题

    // 医生相关 404-409
    DOCTOR_NOT_EXIST(404, "医生不存在"),
    DOCTOR_HAS_SCHEDULE(409, "医生已有排班"),

    // 排班相关 404-409
    SCHEDULE_NOT_EXIST(404, "排班不存在"),
    SCHEDULE_FULL(409, "排班已满"),
    SCHEDULE_CONFLICT(409, "排班时间冲突"),

    // 系统错误 500-599
    SYSTEM_ERROR(500, "系统内部异常"),
    DATABASE_ERROR(500, "数据库操作异常"),
    NETWORK_ERROR(503, "网络异常"), // 503 服务不可用更适合网络问题
    OPERATION_ERROR(500, "操作失败"),
    NOT_FOUND_ERROR(404, "未找到该资源");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
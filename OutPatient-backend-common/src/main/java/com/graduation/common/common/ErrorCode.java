package com.graduation.common.common;

/**
 * 自定义错误码
 */
public enum ErrorCode {
    // 成功
    SUCCESS(0, "ok"),

    // 通用错误 40000-40099
    PARAMS_ERROR(40000, "请求参数错误"),
    NULL_ERROR(40001, "请求数据为空"),
    DATA_EXISTS(40002, "数据已存在"),
    DATA_NOT_EXISTS(40003, "数据不存在"),

    // 认证授权 40100-40199
    NOT_LOGIN(40100, "未登录"),
    NO_AUTH(40101, "无权限"),
    TOKEN_EXPIRED(40102, "Token已过期"),
    TOKEN_INVALID(40103, "Token无效"),

    // 用户相关 40200-40299
    USER_NOT_EXIST(40200, "用户不存在"),
    USER_EXISTS(40201, "用户已存在"),
    PASSWORD_ERROR(40202, "密码错误"),

    // 医生相关 40300-40399
    DOCTOR_NOT_EXIST(40300, "医生不存在"),
    DOCTOR_HAS_SCHEDULE(40301, "医生已有排班"),

    // 排班相关 40400-40499
    SCHEDULE_NOT_EXIST(40400, "排班不存在"),
    SCHEDULE_FULL(40401, "排班已满"),
    SCHEDULE_CONFLICT(40402, "排班时间冲突"),

    // 系统错误 50000-50099
    SYSTEM_ERROR(50000, "系统内部异常"),
    DATABASE_ERROR(50001, "数据库操作异常"),
    NETWORK_ERROR(50002, "网络异常"),
    OPERATION_ERROR(50003, "操作失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

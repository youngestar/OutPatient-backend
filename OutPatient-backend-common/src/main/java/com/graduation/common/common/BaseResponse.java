package com.graduation.common.common;

import lombok.Data;

import java.io.Serializable;


/**
 * 统一响应结果
 */
@Data
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    // 无参构造函数
    public BaseResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    // 全参构造函数
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // 错误码构造函数
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    // 成功响应静态方法
    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), data, "ok");
    }

    public static <T> BaseResponse<T> ok(T data, String message) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), data, message);
    }
}

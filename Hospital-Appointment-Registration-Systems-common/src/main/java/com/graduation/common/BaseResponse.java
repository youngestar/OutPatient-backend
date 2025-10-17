package com.graduation.common;

import java.io.Serializable;


/**
 * 统一响应结果
 */
public class BaseResponse<T> implements Serializable {
    // 序列化版本号
    private static final long serialVersionUID = 1L;
    // 响应码
    private int code;
    // 响应信息
    private String message;
    // 响应数据
    private T data;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

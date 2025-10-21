package com.graduation.common.common;

/**
 * 统一返回工具类
 */
public class ResultUtils {

    /**
     * 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.ok(data);
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.ok(data, message);
    }

    public static <T> BaseResponse<T> success() {
        return BaseResponse.ok(null);
    }

    /**
     * 失败响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 参数错误
     */
    public static <T> BaseResponse<T> paramError(String message) {
        return error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 业务错误
     */
    public static <T> BaseResponse<T> businessError(String message) {
        return error(ErrorCode.OPERATION_ERROR, message);
    }
}

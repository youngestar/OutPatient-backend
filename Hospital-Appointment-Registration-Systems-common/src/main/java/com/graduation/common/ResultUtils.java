package com.graduation.common;

/**
 * 统一返回工具类
 */
public class ResultUtils {
    /**
     * 成功
     * @param data  数据
     * @param <T>  数据类型
     * @return BaseResponse
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, data, "ok");
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return BaseResponse
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code 错误码
     * @param message 错误信息
     * @return BaseResponse
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

}

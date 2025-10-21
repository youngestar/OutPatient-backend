package com.graduation.common.exception;

import com.graduation.common.common.ErrorCode;

public class BusinessException extends RuntimeException {
    private final int code;
    private final String detail;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.detail = null;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.detail = null;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.detail = detail;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.detail = null;
    }

}
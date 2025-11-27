package com.std.cuit.common.exception;

import com.std.cuit.common.common.BaseResponse;
import com.std.cuit.common.common.ErrorCode;
import com.std.cuit.common.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */

@RestControllerAdvice
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: 请求路径: {}, 错误码: {}, 错误信息: {}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@RequestBody参数校验）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数校验异常: 请求路径: {}, 错误信息: {}", request.getRequestURI(), errorMessage);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    /**
     * 处理参数绑定异常（@ModelAttribute参数校验）
     */
    @ExceptionHandler(BindException.class)
    public BaseResponse<?> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数绑定异常: 请求路径: {}, 错误信息: {}", request.getRequestURI(), errorMessage);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    /**
     * 处理参数校验异常（@RequestParam、@PathVariable参数校验）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BaseResponse<?> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数约束异常: 请求路径: {}, 错误信息: {}", request.getRequestURI(), errorMessage);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public BaseResponse<?> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String errorMessage = String.format("参数 '%s' 类型不匹配，期望类型: %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");

        log.warn("参数类型不匹配: 请求路径: {}, 错误信息: {}", request.getRequestURI(), errorMessage);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public BaseResponse<?> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("接口不存在: 请求路径: {}, 请求方法: {}", e.getRequestURL(), e.getHttpMethod());
        return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: 请求路径: {}, 异常信息: {}", request.getRequestURI(), e.getMessage(), e);

        // 根据环境返回不同的错误信息
        String errorMessage = "系统内部异常，请联系管理员";
        // 如果是开发环境，可以返回详细的错误信息
        // if (isDevEnvironment()) {
        //     errorMessage = e.getMessage();
        // }

        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, errorMessage);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public BaseResponse<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: 请求路径: {}", request.getRequestURI(), e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统数据异常");
    }

    /**
     * 处理数据库操作异常
     */
    @ExceptionHandler(DataAccessException.class)
    public BaseResponse<?> handleDataAccessException(
            org.springframework.dao.DataAccessException e, HttpServletRequest request) {
        log.error("数据库操作异常: 请求路径: {}", request.getRequestURI(), e);
        return ResultUtils.error(ErrorCode.DATABASE_ERROR, "数据库操作异常");
    }
}
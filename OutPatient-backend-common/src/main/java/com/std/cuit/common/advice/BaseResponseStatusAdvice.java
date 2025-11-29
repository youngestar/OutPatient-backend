package com.std.cuit.common.advice;

import com.std.cuit.common.common.BaseResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Synchronize HTTP status with BaseResponse.code when controllers return BaseResponse directly.
 */
@RestControllerAdvice
public class BaseResponseStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResponseEntity<?> responseEntity) {
            Object entityBody = responseEntity.getBody();
            if (entityBody instanceof BaseResponse<?> baseResponse) {
                setStatus(response, baseResponse.getCode());
            }
            return body;
        }

        if (body instanceof BaseResponse<?> baseResponse) {
            setStatus(response, baseResponse.getCode());
        }
        return body;
    }

    private void setStatus(ServerHttpResponse response, int code) {
        HttpStatus status = resolveStatus(code);
        if (response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setStatus(status.value());
        } else {
            response.setStatusCode(status);
        }
    }

    private HttpStatus resolveStatus(int code) {
        try {
            return HttpStatus.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return HttpStatus.OK;
        }
    }
}

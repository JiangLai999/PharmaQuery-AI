package com.pharmacy.config;

import com.pharmacy.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理 - 捕获所有未处理异常，返回具体错误信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<String> handleException(Exception e) {
        log.error("接口异常: ", e);
        // 返回具体错误信息便于调试
        String message = e.getMessage();
        if (e.getCause() != null) {
            message = message + " -> " + e.getCause().getMessage();
        }
        return ApiResult.error(500, message);
    }
}

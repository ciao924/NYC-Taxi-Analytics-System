package com.taxi.analytics.common.exception;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, org.springframework.validation.BindException.class})
    public Result<?> handleValidationException(Exception e) {
        String message = "参数校验异常";
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (e instanceof org.springframework.validation.BindException) {
            message = ((org.springframework.validation.BindException) e).getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        log.warn("参数校验异常: {}", message);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统内部异常", e);
        return Result.error(ResultCode.INTERNAL_ERROR.getCode(), ResultCode.INTERNAL_ERROR.getMessage());
    }
}
package com.taxi.analytics.common.exception;

import com.taxi.analytics.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.INTERNAL_ERROR.getCode();
    }
    
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
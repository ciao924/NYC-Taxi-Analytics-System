package com.taxi.analytics.common.result;

import java.io.Serializable;

public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private Boolean success;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
        this.success = false;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public Result<T> setCode(Integer code) {
        this.code = code;
        return this;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Result<T> setMessage(String message) {
        this.message = message;
        return this;
    }
    
    public T getData() {
        return data;
    }
    
    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public Result<T> setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public Result<T> setSuccess(Boolean success) {
        this.success = success;
        return this;
    }
    
    public static <T> Result<T> success() {
        return success(null);
    }
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setSuccess(true);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        return error(ResultCode.INTERNAL_ERROR.getCode(), message);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setSuccess(false);
        return result;
    }
}
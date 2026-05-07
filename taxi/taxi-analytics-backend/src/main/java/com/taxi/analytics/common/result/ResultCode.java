package com.taxi.analytics.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    
    // 成功
    SUCCESS(200, "操作成功"),
    
    // 客户端错误 (400-499)
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    
    // 服务端错误 (500-599)
    INTERNAL_ERROR(500, "系统内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    
    // 业务错误码 (1000-1999)
    DATE_RANGE_INVALID(1001, "日期范围无效"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    PARAM_MISSING(1003, "缺少必要参数"),
    PARAM_INVALID(1004, "参数格式错误"),
    DATE_RANGE_TOO_LARGE(1005, "日期范围超过限制"),
    
    // 导出错误码 (2000-2099)
    EXPORT_FAILED(2001, "导出失败"),
    EXPORT_TOO_MANY_ROWS(2002, "导出数据量超过限制"),
    EXPORT_TASK_NOT_FOUND(2003, "导出任务不存在"),
    EXPORT_TASK_EXPIRED(2004, "导出任务已过期"),
    EXPORT_CONCURRENT_LIMIT(2005, "导出任务过多，请稍后重试");
    
    private final Integer code;
    private final String message;
    
    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
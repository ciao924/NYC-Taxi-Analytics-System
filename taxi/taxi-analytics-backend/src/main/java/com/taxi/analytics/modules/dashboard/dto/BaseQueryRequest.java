package com.taxi.analytics.modules.dashboard.dto;

import com.taxi.analytics.common.validation.DateRange;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@DateRange(startField = "startDate", endField = "endDate")
public class BaseQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "开始日期不能为空")
    private String startDate;

    @NotBlank(message = "结束日期不能为空")
    private String endDate;
}
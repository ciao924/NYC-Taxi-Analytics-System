package com.taxi.analytics.modules.ai.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 图表配置 DTO
 * 严格遵循 API 契约，字段名使用下划线格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartConfig {

    /**
     * 图表类型
     * 取值仅限: line, bar, pie, stacked_bar, table, horizontal_bar
     */
    @JsonProperty("chart_type")
    private String chartType;

    /**
     * 标题
     */
    @JsonProperty("title")
    private String title;

    /**
     * X轴字段名（对应 data 数组中对象的字段名）
     */
    @JsonProperty("x_field")
    private String xField;

    /**
     * Y轴字段名（或字段名数组，对应 data 数组中对象的字段名）
     */
    @JsonProperty("y_field")
    private Object yField;

    /**
     * 是否显示图例
     */
    @JsonProperty("legend")
    private Boolean legend;

    /**
     * 是否显示百分比
     */
    @JsonProperty("percentage")
    private Boolean percentage;

    /**
     * 图表数据（后端执行 SQL 后得到的实际数据数组）
     */
    @JsonProperty("data")
    private List<Map<String, Object>> data;

    /**
     * 获取有效的图表类型
     */
    public String getValidChartType() {
        if (chartType == null || chartType.isEmpty()) {
            return "bar";
        }
        // 验证图表类型是否在允许范围内
        String[] validTypes = {"line", "bar", "pie", "stacked_bar", "table", "horizontal_bar"};
        for (String type : validTypes) {
            if (type.equalsIgnoreCase(chartType)) {
                return type;
            }
        }
        return "bar";
    }
}
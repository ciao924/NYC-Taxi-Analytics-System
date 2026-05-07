package com.taxi.analytics.modules.ai.model;

import com.taxi.analytics.modules.ai.dsl.Dsl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ChartBuilder {

    private static final Logger log = LoggerFactory.getLogger(ChartBuilder.class);
    
    public ChartData build(List<Map<String, Object>> rows, Dsl dsl) {
        log.info("开始构建图表数据，数据行数：{}", rows.size());

        ChartData chartData = new ChartData();
        List<String> x = new ArrayList<>();
        List<Number> y = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            log.warn("查询结果为空，返回空图表数据");
            chartData.setX(x);
            chartData.setY(y);
            return chartData;
        }

        for (Map<String, Object> row : rows) {
            Object dimensionValue = row.get("dimension");
            Object value = row.get("value");

            if (dimensionValue != null) {
                x.add(dimensionValue.toString());
            }

            if (value != null) {
                if (value instanceof Number) {
                    y.add((Number) value);
                } else if (value instanceof BigDecimal) {
                    y.add(((BigDecimal) value).doubleValue());
                } else {
                    try {
                        y.add(Double.parseDouble(value.toString()));
                    } catch (NumberFormatException e) {
                        log.warn("无法解析数值：{}，使用 0 代替", value);
                        y.add(0);
                    }
                }
            }
        }

        chartData.setX(x);
        chartData.setY(y);

        log.info("图表数据构建完成，X 轴数据点数：{}，Y 轴数据点数：{}", x.size(), y.size());
        return chartData;
    }
}
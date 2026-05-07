package com.taxi.analytics.modules.ai.service;

import com.taxi.analytics.modules.ai.intent.Intent;
import com.taxi.analytics.modules.ai.intent.IntentType;
import com.taxi.analytics.modules.ai.service.dto.ChartConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SqlGenerator {

    private static final Logger log = LoggerFactory.getLogger(SqlGenerator.class);

    public String generateSql(String query, Map<String, Object> schemaInfo, Intent intent) {
        log.info("Generating SQL for query: {}, intent: {}", query, intent.getIntentType());

        // 根据意图类型生成不同的SQL
        switch (intent.getIntentType()) {
            case DATA_QUERY:
                return generateDataQuerySql(query, schemaInfo, intent);
            case ETL_GENERATION:
                return generateEtlSql(query, schemaInfo);
            default:
                return "SELECT * FROM dwd_taxi_trip LIMIT 10";
        }
    }

    private String generateDataQuerySql(String query, Map<String, Object> schemaInfo, Intent intent) {
        // 简单的SQL生成逻辑
        String sql = "SELECT ";

        // 提取实体
        Map<String, String> entities = intent.getEntities();
        String metric = entities.get("metric");
        String dimension = entities.get("dimension");
        String time = entities.get("time");

        // 构建SELECT子句
        if (metric != null) {
            if (metric.contains("订单")) {
                sql += "COUNT(*) as trip_count";
            } else if (metric.contains("金额")) {
                sql += "SUM(total_amount) as total_amount, AVG(total_amount) as avg_amount";
            } else {
                sql += "COUNT(*) as count";
            }
        } else {
            sql += "COUNT(*) as count";
        }

        // 构建GROUP BY子句
        if (dimension != null) {
            sql += ", ";
            if (dimension.contains("供应商")) {
                sql += "vendor_id";
            } else if (dimension.contains("支付")) {
                sql += "payment_type";
            } else if (dimension.contains("时间")) {
                sql += "DATE(pickup_datetime) as pickup_date";
            } else {
                sql += "vendor_id";
            }
            sql += " GROUP BY " + (dimension.contains("时间") ? "DATE(pickup_datetime)" : dimension.contains("供应商") ? "vendor_id" : "payment_type");
        }

        // 构建WHERE子句
        sql += " FROM dwd_taxi_trip";
        if (time != null) {
            sql += " WHERE ";
            if (time.contains("今天")) {
                sql += "DATE(pickup_datetime) = CURRENT_DATE";
            } else if (time.contains("昨天")) {
                sql += "DATE(pickup_datetime) = CURRENT_DATE - INTERVAL 1 DAY";
            } else if (time.contains("本周")) {
                sql += "WEEK(pickup_datetime) = WEEK(CURRENT_DATE)";
            } else if (time.contains("本月")) {
                sql += "MONTH(pickup_datetime) = MONTH(CURRENT_DATE)";
            }
        }

        // 构建ORDER BY子句
        sql += " ORDER BY " + (metric != null && metric.contains("金额") ? "total_amount DESC" : "trip_count DESC");
        sql += " LIMIT 10";

        return sql;
    }

    private String generateEtlSql(String query, Map<String, Object> schemaInfo) {
        // 简单的ETL SQL生成
        return "INSERT INTO dwd_taxi_trip_cleaned " +
               "SELECT trip_id, vendor_id, pickup_datetime, dropoff_datetime, " +
               "passenger_count, trip_distance, pickup_location_id, dropoff_location_id, " +
               "payment_type, fare_amount, extra, tip_amount, tolls_amount, " +
               "improvement_surcharge, total_amount, dt " +
               "FROM dwd_taxi_trip " +
               "WHERE passenger_count > 0 AND trip_distance > 0";
    }

    public String explainSql(String sql) {
        return "这是一个分析SQL查询，用于分析出租车数据。";
    }

    public ChartConfig suggestChart(String sql, List<Map<String, Object>> data) {
        ChartConfig config = new ChartConfig();
        
        // 根据SQL内容建议图表类型
        if (sql.contains("GROUP BY")) {
            if (sql.contains("DATE(pickup_datetime)")) {
                config.setChartType("line");
                config.setXAxisField("pickup_date");
            } else {
                config.setChartType("bar");
                if (sql.contains("vendor_id")) {
                    config.setXAxisField("vendor_id");
                } else if (sql.contains("payment_type")) {
                    config.setXAxisField("payment_type");
                }
            }
        } else {
            config.setChartType("table");
        }

        // 设置Y轴字段
        if (sql.contains("trip_count")) {
            config.setYAxisField("trip_count");
        } else if (sql.contains("total_amount")) {
            config.setYAxisField("total_amount");
        } else {
            config.setYAxisField("count");
        }

        // 设置数据
        List<Map<String, Object>> chartData = new ArrayList<>();
        String xField = config.getXAxisField();
        String yField = config.getYAxisField();
        
        for (Map<String, Object> row : data) {
            Map<String, Object> chartItem = new HashMap<>();
            chartItem.put("name", row.get(xField));
            chartItem.put("value", row.get(yField));
            chartData.add(chartItem);
        }
        config.setData(chartData);

        config.setTitle("数据分析结果");
        return config;
    }
}

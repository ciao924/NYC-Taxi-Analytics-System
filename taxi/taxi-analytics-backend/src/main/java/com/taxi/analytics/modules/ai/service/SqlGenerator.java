package com.taxi.analytics.modules.ai.service;

import com.taxi.analytics.modules.ai.dsl.Dsl;
import com.taxi.analytics.modules.ai.intent.Intent;
import com.taxi.analytics.modules.ai.intent.IntentType;
import com.taxi.analytics.modules.ai.service.dto.ChartConfig;
import com.taxi.analytics.modules.ai.service.SchemaRetriever.ColumnInfo;
import com.taxi.analytics.modules.ai.service.SchemaRetriever.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SqlGenerator - SQL生成服务
 * 根据自然语言意图或DSL生成精准的SQL查询语句
 * 支持查询MySQL库18张ADS层表
 */
@Service
public class SqlGenerator {

    private static final Logger log = LoggerFactory.getLogger(SqlGenerator.class);

    private final SchemaRetriever schemaRetriever;

    // 指标到字段的映射
    private static final Map<String, String> METRIC_FIELD_MAP = new HashMap<>();
    // 维度到字段的映射
    private static final Map<String, String> DIMENSION_FIELD_MAP = new HashMap<>();
    // 维度到表名的映射
    private static final Map<String, String> DIMENSION_TABLE_MAP = new HashMap<>();

    static {
        // 初始化指标映射
        METRIC_FIELD_MAP.put("total_trips", "total_trips");
        METRIC_FIELD_MAP.put("trip_count", "trip_count");
        METRIC_FIELD_MAP.put("total_revenue", "total_revenue");
        METRIC_FIELD_MAP.put("total_amount", "total_amount");
        METRIC_FIELD_MAP.put("total_fare", "total_fare");
        METRIC_FIELD_MAP.put("avg_fare", "avg_fare");
        METRIC_FIELD_MAP.put("total_tip", "total_tip");
        METRIC_FIELD_MAP.put("avg_tip", "avg_tip");
        METRIC_FIELD_MAP.put("avg_distance", "avg_distance");
        METRIC_FIELD_MAP.put("avg_duration", "avg_duration");
        METRIC_FIELD_MAP.put("percentage", "percentage");
        METRIC_FIELD_MAP.put("ratio", "revenue_ratio");
        METRIC_FIELD_MAP.put("pickup_count", "pickup_count");
        METRIC_FIELD_MAP.put("dropoff_count", "dropoff_count");

        // 初始化维度映射
        DIMENSION_FIELD_MAP.put("stat_date", "stat_date");
        DIMENSION_FIELD_MAP.put("hour_of_day", "hour_of_day");
        DIMENSION_FIELD_MAP.put("day_of_week", "day_of_week");
        DIMENSION_FIELD_MAP.put("day_name", "day_name");
        DIMENSION_FIELD_MAP.put("payment_name", "payment_name");
        DIMENSION_FIELD_MAP.put("vendor_name", "vendor_name");
        DIMENSION_FIELD_MAP.put("taxi_type", "taxi_type");
        DIMENSION_FIELD_MAP.put("borough", "borough");
        DIMENSION_FIELD_MAP.put("pu_borough", "pu_borough");
        DIMENSION_FIELD_MAP.put("do_borough", "do_borough");
        DIMENSION_FIELD_MAP.put("pu_zone", "pu_zone");
        DIMENSION_FIELD_MAP.put("do_zone", "do_zone");
        DIMENSION_FIELD_MAP.put("passenger_count", "passenger_count");
        DIMENSION_FIELD_MAP.put("distance_range", "distance_range");
        DIMENSION_FIELD_MAP.put("duration_range", "duration_range");
        DIMENSION_FIELD_MAP.put("airport_trip", "airport_trip");
        DIMENSION_FIELD_MAP.put("fee_code", "fee_code");
        DIMENSION_FIELD_MAP.put("fee_name", "fee_name");

        // 初始化维度到表的映射
        DIMENSION_TABLE_MAP.put("stat_date", "analysis_kpi_daily");
        DIMENSION_TABLE_MAP.put("hour_of_day", "analysis_hourly_distribution");
        DIMENSION_TABLE_MAP.put("day_of_week", "analysis_weekday_analysis");
        DIMENSION_TABLE_MAP.put("day_name", "analysis_weekday_analysis");
        DIMENSION_TABLE_MAP.put("payment_name", "analysis_payment_analysis");
        DIMENSION_TABLE_MAP.put("vendor_name", "analysis_vendor");
        DIMENSION_TABLE_MAP.put("taxi_type", "analysis_fee_by_taxi_type");
        DIMENSION_TABLE_MAP.put("borough", "analysis_fee_by_borough");
        DIMENSION_TABLE_MAP.put("pu_borough", "analysis_borough_flow");
        DIMENSION_TABLE_MAP.put("do_borough", "analysis_borough_flow");
        DIMENSION_TABLE_MAP.put("pu_zone", "analysis_pickup_hotspots");
        DIMENSION_TABLE_MAP.put("do_zone", "analysis_dropoff_hotspots");
        DIMENSION_TABLE_MAP.put("passenger_count", "analysis_passenger_distribution");
        DIMENSION_TABLE_MAP.put("distance_range", "analysis_distance_distribution");
        DIMENSION_TABLE_MAP.put("duration_range", "analysis_duration_distribution");
        DIMENSION_TABLE_MAP.put("airport_trip", "analysis_airport");
        DIMENSION_TABLE_MAP.put("fee_code", "analysis_fee_composition");
        DIMENSION_TABLE_MAP.put("fee_name", "analysis_fee_composition");
    }

    public SqlGenerator(SchemaRetriever schemaRetriever) {
        this.schemaRetriever = schemaRetriever;
    }

    /**
     * 根据自然语言查询和意图生成SQL（已废弃，使用DSL流程）
     * @deprecated 使用 {@link #generateSqlFromDsl(Dsl)} 替代
     */
    @Deprecated
    public String generateSql(String query, Map<String, Object> schemaInfo, Intent intent) {
        log.warn("generateSql方法已废弃，请使用generateSqlFromDsl方法");
        
        // 构建默认DSL并生成SQL
        Dsl dsl = new Dsl();
        dsl.setMetric("total_trips");
        dsl.setDimension("stat_date");
        dsl.setTimeRange("7d");
        
        return generateSqlFromDsl(dsl);
    }

    /**
     * 根据DSL生成SQL（核心方法）
     */
    public String generateSqlFromDsl(Dsl dsl) {
        log.info("根据DSL生成SQL: metric={}, dimension={}, timeRange={}", 
                dsl.getMetric(), dsl.getDimension(), dsl.getTimeRange());

        // 1. 确定目标表
        String tableName = determineTableName(dsl.getDimension(), dsl.getMetric());
        log.info("确定目标表: {}", tableName);

        // 2. 从SchemaRetriever获取表结构
        TableInfo tableInfo = schemaRetriever.getTableSchema(tableName);
        if (tableInfo == null) {
            log.error("无法获取表结构信息: {}", tableName);
            throw new RuntimeException("表结构不存在: " + tableName);
        }

        // 3. 根据表结构确定指标字段和维度字段
        String metricField = resolveMetricField(tableInfo, dsl.getMetric());
        String dimensionField = resolveDimensionField(tableInfo, dsl.getDimension());
        
        log.info("解析字段 - metricField: {}, dimensionField: {}", metricField, dimensionField);

        // 4. 构建SQL
        StringBuilder sql = new StringBuilder();
        
        // SELECT子句 - 始终包含维度字段
        sql.append("SELECT ");
        if (dimensionField != null) {
            sql.append(dimensionField).append(", ");
        }
        sql.append("SUM(").append(metricField).append(") AS value ");
        
        // FROM子句
        sql.append("FROM ").append(tableName).append(" ");
        
        // WHERE子句 - 时间过滤
        String timeCondition = buildTimeCondition(dsl.getTimeRange());
        if (timeCondition != null && !timeCondition.isEmpty()) {
            sql.append("WHERE ").append(timeCondition).append(" ");
        }
        
        // WHERE子句 - 额外过滤条件
        if (dsl.getFilters() != null && !dsl.getFilters().isEmpty()) {
            if (timeCondition != null && !timeCondition.isEmpty()) {
                sql.append("AND ");
            } else {
                sql.append("WHERE ");
            }
            sql.append(buildFilterConditions(dsl.getFilters())).append(" ");
        }
        
        // GROUP BY子句 - 始终按维度字段分组
        if (dimensionField != null) {
            sql.append("GROUP BY ").append(dimensionField).append(" ");
        }
        
        // ORDER BY子句 - 根据维度类型选择排序方式
        if (dimensionField != null && (dimensionField.equals("stat_date") || 
                                       dimensionField.equals("hour_of_day") || 
                                       dimensionField.equals("day_of_week"))) {
            // 时间维度按时间升序排列
            sql.append("ORDER BY ").append(dimensionField).append(" ASC ");
        } else {
            // 非时间维度按值降序排列
            sql.append("ORDER BY value DESC ");
        }
        
        // LIMIT子句
        sql.append("LIMIT 50");

        String finalSql = sql.toString();
        log.info("生成SQL: {}", finalSql);
        return finalSql;
    }
    
    /**
     * 根据表结构和指标类型解析指标字段
     */
    private String resolveMetricField(TableInfo tableInfo, String metricType) {
        // 建立指标类型到字段的映射（基于表结构）
        Map<String, List<String>> metricToFields = new HashMap<>();
        metricToFields.put("total_trips", Arrays.asList("total_trips", "trip_count", "pickup_count", "dropoff_count"));
        metricToFields.put("trip_count", Arrays.asList("trip_count", "total_trips", "pickup_count", "dropoff_count"));
        metricToFields.put("total_revenue", Arrays.asList("total_revenue", "total_amount"));
        metricToFields.put("total_amount", Arrays.asList("total_amount", "total_revenue"));
        metricToFields.put("total_fare", Arrays.asList("total_fare"));
        metricToFields.put("avg_fare", Arrays.asList("avg_fare"));
        metricToFields.put("total_tip", Arrays.asList("total_tip"));
        metricToFields.put("avg_tip", Arrays.asList("avg_tip"));
        metricToFields.put("avg_distance", Arrays.asList("avg_distance"));
        metricToFields.put("avg_duration", Arrays.asList("avg_duration"));
        
        List<String> candidateFields = metricToFields.getOrDefault(metricType, Arrays.asList(metricType));
        
        // 在表结构中查找匹配的字段
        for (String field : candidateFields) {
            for (ColumnInfo col : tableInfo.getColumns()) {
                if (col.getName().equalsIgnoreCase(field)) {
                    return col.getName();
                }
            }
        }
        
        // 如果没找到，返回原始metricType
        log.warn("未找到匹配的指标字段，使用默认值: {}", metricType);
        return candidateFields.get(0);
    }
    
    /**
     * 根据表结构和维度类型解析维度字段
     */
    private String resolveDimensionField(TableInfo tableInfo, String dimensionType) {
        // 维度字段通常是维度类型的直接映射
        List<String> candidateFields = Arrays.asList(dimensionType);
        
        // 在表结构中查找匹配的字段
        for (String field : candidateFields) {
            for (ColumnInfo col : tableInfo.getColumns()) {
                if (col.getName().equalsIgnoreCase(field)) {
                    return col.getName();
                }
            }
        }
        
        // 如果没找到，返回stat_date作为默认维度
        log.warn("未找到匹配的维度字段，使用默认值: stat_date");
        return "stat_date";
    }

    /**
     * 根据维度和指标确定目标表
     */
    private String determineTableName(String dimension, String metric) {
        // 优先根据维度确定表
        if (dimension != null && DIMENSION_TABLE_MAP.containsKey(dimension)) {
            return DIMENSION_TABLE_MAP.get(dimension);
        }

        // 如果维度是日期，根据指标确定表
        if ("stat_date".equals(dimension) || "date".equals(dimension)) {
            if (metric != null) {
                // 根据指标类型选择表
                if (metric.contains("fare") || metric.contains("tip") || metric.contains("revenue")) {
                    return "analysis_kpi_daily";
                } else if (metric.contains("trip")) {
                    return "analysis_kpi_daily";
                }
            }
            return "analysis_kpi_daily";
        }

        // 默认返回KPI日报表
        return "analysis_kpi_daily";
    }

    /**
     * 构建时间条件
     */
    private String buildTimeCondition(String timeRange) {
        if (timeRange == null || timeRange.isEmpty()) {
            return "";
        }

        // 处理"7d"格式
        if (timeRange.endsWith("d")) {
            int days = Integer.parseInt(timeRange.substring(0, timeRange.length() - 1));
            return String.format("stat_date >= DATE_SUB(CURDATE(), INTERVAL %d DAY)", days);
        }

        // 处理日期范围格式 "2025-01-01,2025-01-31"
        if (timeRange.contains(",")) {
            String[] dates = timeRange.split(",");
            if (dates.length == 2) {
                return String.format("stat_date BETWEEN '%s' AND '%s'", dates[0], dates[1]);
            }
        }

        // 处理单个日期
        if (timeRange.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}")) {
            // 统一日期格式
            String normalizedDate = timeRange.replace("/", "-");
            return String.format("stat_date = '%s'", normalizedDate);
        }

        // 默认返回最近7天
        return "stat_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
    }

    /**
     * 构建过滤条件
     */
    private String buildFilterConditions(Map<String, Object> filters) {
        List<String> conditions = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if ("taxi_type".equals(key)) {
                conditions.add("taxi_type = '" + value + "'");
            } else if ("borough".equals(key)) {
                conditions.add("borough = '" + value + "'");
            } else if ("pu_borough".equals(key)) {
                conditions.add("pu_borough = '" + value + "'");
            } else if ("do_borough".equals(key)) {
                conditions.add("do_borough = '" + value + "'");
            } else if ("payment_type".equals(key)) {
                conditions.add("payment_type = '" + value + "'");
            }
        }
        
        return String.join(" AND ", conditions);
    }

    /**
     * 生成默认SQL（已废弃，使用DSL流程）
     * @deprecated 使用 {@link #generateSqlFromDsl(Dsl)} 替代
     */
    @Deprecated
    private String generateDefaultSql() {
        log.warn("generateDefaultSql方法已废弃，请使用generateSqlFromDsl方法");
        
        Dsl dsl = new Dsl();
        dsl.setMetric("total_trips");
        dsl.setDimension("stat_date");
        dsl.setTimeRange("7d");
        
        return generateSqlFromDsl(dsl);
    }

    /**
     * 解释SQL含义
     */
    public String explainSql(String sql) {
        StringBuilder explanation = new StringBuilder("这是一个数据分析查询，");
        
        if (sql.contains("COUNT")) {
            explanation.append("用于统计数据数量");
        } else if (sql.contains("SUM")) {
            explanation.append("用于计算数据总和");
        } else if (sql.contains("AVG")) {
            explanation.append("用于计算平均值");
        }
        
        if (sql.contains("GROUP BY")) {
            explanation.append("，按指定维度分组");
        }
        
        if (sql.contains("ORDER BY")) {
            explanation.append("，结果已排序");
        }
        
        explanation.append("。");
        return explanation.toString();
    }

    /**
     * 根据SQL和数据建议图表类型
     */
    public ChartConfig suggestChart(String sql, List<Map<String, Object>> data) {
        ChartConfig config = new ChartConfig();
        
        // 根据SQL内容建议图表类型
        if (sql.contains("GROUP BY")) {
            if (sql.contains("stat_date") || sql.contains("pickup_date")) {
                config.setChartType("line");
                config.setXField("stat_date");
            } else {
                config.setChartType("bar");
                // 根据维度字段设置 X 轴
                if (sql.contains("vendor_id") || sql.contains("vendor_name")) {
                    config.setXField("vendor_name");
                } else if (sql.contains("payment_type") || sql.contains("payment_name")) {
                    config.setXField("payment_name");
                } else if (sql.contains("borough")) {
                    config.setXField("borough");
                } else if (sql.contains("pu_zone")) {
                    config.setXField("pu_zone");
                } else if (sql.contains("taxi_type")) {
                    config.setXField("taxi_type");
                }
            }
        } else {
            config.setChartType("table");
        }

        // 设置 Y 轴字段
        if (sql.contains("trip_count") || sql.contains("total_trips")) {
            config.setYField("trip_count");
        } else if (sql.contains("total_revenue") || sql.contains("total_amount")) {
            config.setYField("total_revenue");
        } else if (sql.contains("avg_fare")) {
            config.setYField("avg_fare");
        } else {
            config.setYField("count");
        }

        // 设置数据
        List<Map<String, Object>> chartData = new ArrayList<>();
        String xField = config.getXField() != null ? config.getXField() : "name";
        String yField = config.getYField() != null ? config.getYField().toString() : "value";
        
        for (Map<String, Object> row : data) {
            Map<String, Object> chartItem = new HashMap<>();
            
            // 尝试获取X轴值
            Object xValue = row.get(xField);
            if (xValue == null) {
                // 如果没有指定的xField，尝试其他可能的字段
                xValue = row.get("stat_date");
                if (xValue == null) xValue = row.get("name");
                if (xValue == null && !row.isEmpty()) {
                    xValue = row.keySet().iterator().next();
                }
            }
            chartItem.put("name", xValue != null ? xValue.toString() : "");
            
            // 尝试获取Y轴值
            Object yValue = row.get(yField);
            if (yValue == null) {
                // 如果没有指定的yField，尝试获取数值类型的字段
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        yValue = entry.getValue();
                        break;
                    }
                }
            }
            chartItem.put("value", yValue != null ? yValue : 0);
            
            chartData.add(chartItem);
        }
        config.setData(chartData);

        // 设置标题
        config.setTitle("数据分析结果");
        
        log.info("生成图表配置：chartType={}, xAxis={}, yAxis={}, dataSize={}", 
                config.getChartType(), config.getXField(), config.getYField(), chartData.size());
        
        return config;
    }

    /**
     * 根据DSL建议图表类型
     */
    public ChartConfig suggestChartFromDsl(Dsl dsl) {
        ChartConfig config = new ChartConfig();
        
        // 设置图表类型 - 根据维度类型自动选择
        String chartType = dsl.getChart();
        if (chartType == null || chartType.isEmpty()) {
            // 时间维度使用折线图，其他维度使用柱状图
            String dimension = dsl.getDimension();
            if (dimension != null && (dimension.equals("stat_date") || 
                                      dimension.equals("hour_of_day") || 
                                      dimension.equals("day_of_week"))) {
                chartType = "line";
            } else {
                chartType = "bar";
            }
        }
        config.setChartType(chartType);
        
        // 设置轴字段 - SQL 查询结果中维度字段保持原样，值字段统一为"value"
        config.setXField(dsl.getDimension());
        config.setYField("value");  // SQL 中 SUM 结果字段名为 value
        
        // 设置标题
        config.setTitle(buildChartTitle(dsl));
        
        return config;
    }

    /**
     * 根据DSL构建图表标题
     */
    private String buildChartTitle(Dsl dsl) {
        StringBuilder title = new StringBuilder();
        
        // 添加指标名称
        String metricName = getMetricName(dsl.getMetric());
        title.append(metricName);
        
        // 添加维度名称
        String dimensionName = getDimensionName(dsl.getDimension());
        if (!"日期".equals(dimensionName)) {
            title.append(" - ").append(dimensionName);
        }
        
        // 添加时间范围
        String timeRange = dsl.getTimeRange();
        if (timeRange != null && !timeRange.isEmpty()) {
            title.append(" (").append(formatTimeRange(timeRange)).append(")");
        }
        
        return title.toString();
    }

    /**
     * 获取指标中文名称
     */
    private String getMetricName(String metric) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("total_trips", "订单总量");
        mapping.put("trip_count", "订单数");
        mapping.put("total_revenue", "总收入");
        mapping.put("total_amount", "总金额");
        mapping.put("total_fare", "总车费");
        mapping.put("avg_fare", "平均车费");
        mapping.put("total_tip", "总小费");
        mapping.put("avg_tip", "平均小费");
        mapping.put("avg_distance", "平均距离");
        mapping.put("avg_duration", "平均时长");
        mapping.put("percentage", "占比");
        mapping.put("ratio", "比率");
        return mapping.getOrDefault(metric, metric);
    }

    /**
     * 获取维度中文名称
     */
    private String getDimensionName(String dimension) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("stat_date", "日期");
        mapping.put("hour_of_day", "小时");
        mapping.put("day_of_week", "星期");
        mapping.put("payment_name", "支付方式");
        mapping.put("vendor_name", "供应商");
        mapping.put("taxi_type", "出租车类型");
        mapping.put("borough", "行政区");
        mapping.put("pu_zone", "上车区域");
        mapping.put("do_zone", "下车区域");
        mapping.put("passenger_count", "乘客人数");
        mapping.put("distance_range", "距离区间");
        mapping.put("duration_range", "时长区间");
        mapping.put("airport_trip", "机场类型");
        return mapping.getOrDefault(dimension, dimension);
    }

    /**
     * 格式化时间范围显示
     */
    private String formatTimeRange(String timeRange) {
        if (timeRange.endsWith("d")) {
            int days = Integer.parseInt(timeRange.substring(0, timeRange.length() - 1));
            return "最近" + days + "天";
        }
        
        if (timeRange.contains(",")) {
            String[] dates = timeRange.split(",");
            if (dates.length == 2) {
                return dates[0] + "至" + dates[1];
            }
        }
        
        return timeRange;
    }
}

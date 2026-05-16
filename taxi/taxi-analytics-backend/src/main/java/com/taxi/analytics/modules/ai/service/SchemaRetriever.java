package com.taxi.analytics.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SchemaRetriever - ADS层表结构检索服务
 * 管理18张ADS层表的元数据，支持AI智能查询的SQL生成
 * 
 * 表结构定义基于 ADS_Table_Schema1.2.md
 * 数据时间范围: 2025年1月1日 至 2025年3月31日
 */
@Service
public class SchemaRetriever {

    private static final Logger log = LoggerFactory.getLogger(SchemaRetriever.class);

    private final Map<String, SchemaInfo> schemaCache = new HashMap<>();

    public SchemaRetriever() {
        initializeSchema();
    }

    /**
     * 初始化18张ADS层表结构
     * 表结构定义基于 ADS_Table_Schema1.2.md
     */
    private void initializeSchema() {
        // 1. KPI日报表 - analysis_kpi_daily
        TableInfo kpiDailyTable = new TableInfo(
            "analysis_kpi_daily",
            "KPI日报表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("total_trips", "double", "总订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("avg_distance", "double", "平均距离(英里)"),
                new ColumnInfo("avg_duration", "double", "平均时长(分钟)"),
                new ColumnInfo("total_tip", "double", "总小费(美元)"),
                new ColumnInfo("avg_tip", "double", "平均小费(美元)"),
                new ColumnInfo("airport_trips", "double", "机场订单数"),
                new ColumnInfo("peak_hours", "text", "高峰时段"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 2. 小时分布表 - analysis_hourly_distribution
        TableInfo hourlyDistributionTable = new TableInfo(
            "analysis_hourly_distribution",
            "小时分布表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("hour_of_day", "int", "小时(0-23)"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("avg_tip", "double", "平均小费(美元)"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 3. 工作日分析表 - analysis_weekday_analysis
        TableInfo weekdayAnalysisTable = new TableInfo(
            "analysis_weekday_analysis",
            "工作日分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("day_of_week", "int", "星期(1-7,1=周一)"),
                new ColumnInfo("day_of_week_name", "text", "星期名称"),
                new ColumnInfo("total_trips", "double", "订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("avg_distance", "double", "平均距离(英里)"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 4. 支付分析表 - analysis_payment_analysis
        TableInfo paymentAnalysisTable = new TableInfo(
            "analysis_payment_analysis",
            "支付方式分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("payment_name", "text", "支付方式名称"),
                new ColumnInfo("is_cashless", "bit", "是否无现金"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("total_amount", "double", "总金额(美元)"),
                new ColumnInfo("avg_amount", "double", "平均金额(美元)"),
                new ColumnInfo("total_tip", "double", "总小费(美元)"),
                new ColumnInfo("avg_tip", "double", "平均小费(美元)"),
                new ColumnInfo("trip_ratio", "double", "订单占比"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 5. 上车热点表 - analysis_pickup_hotspots
        TableInfo pickupHotspotsTable = new TableInfo(
            "analysis_pickup_hotspots",
            "上车热点分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("zone_name", "text", "上车区域名称"),
                new ColumnInfo("borough", "text", "行政区"),
                new ColumnInfo("service_zone", "text", "服务区"),
                new ColumnInfo("trip_count", "double", "上车次数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 6. 下车热点表 - analysis_dropoff_hotspots
        TableInfo dropoffHotspotsTable = new TableInfo(
            "analysis_dropoff_hotspots",
            "下车热点分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("zone_name", "text", "下车区域名称"),
                new ColumnInfo("borough", "text", "行政区"),
                new ColumnInfo("service_zone", "text", "服务区"),
                new ColumnInfo("trip_count", "double", "下车次数"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 7. 区域流量表 - analysis_borough_flow
        TableInfo boroughFlowTable = new TableInfo(
            "analysis_borough_flow",
            "区域流量分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("pu_borough", "text", "上车行政区"),
                new ColumnInfo("do_borough", "text", "下车行政区"),
                new ColumnInfo("pickup_count", "double", "上车次数"),
                new ColumnInfo("dropoff_count", "double", "下车次数"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 8. 费用组成表 - analysis_fee_composition
        TableInfo feeCompositionTable = new TableInfo(
            "analysis_fee_composition",
            "费用组成分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("fee_code", "text", "费用类型编码"),
                new ColumnInfo("fee_name", "text", "费用类型名称"),
                new ColumnInfo("total_amount", "double", "总金额"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 9. 费用百分比表 - analysis_fee_percentage
        TableInfo feePercentageTable = new TableInfo(
            "analysis_fee_percentage",
            "费用百分比分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("fee_code", "text", "费用类型编码"),
                new ColumnInfo("fee_name", "text", "费用类型名称"),
                new ColumnInfo("percentage", "double", "占比"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 10. 区域费用表 - analysis_fee_by_borough
        TableInfo feeByBoroughTable = new TableInfo(
            "analysis_fee_by_borough",
            "区域费用分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("borough", "text", "行政区"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("total_tip", "double", "总小费(美元)"),
                new ColumnInfo("avg_tip", "double", "平均小费(美元)"),
                new ColumnInfo("revenue_ratio", "double", "收入占比"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 11. 费用趋势表 - analysis_fee_trend
        TableInfo feeTrendTable = new TableInfo(
            "analysis_fee_trend",
            "费用趋势表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("taxi_type", "text", "出租车类型"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("total_tip", "double", "总小费(美元)"),
                new ColumnInfo("avg_tip_rate", "double", "平均小费率"),
                new ColumnInfo("cashless_rate", "double", "无现金率"),
                new ColumnInfo("revenue_growth", "double", "收入增长率(%)"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 12. 出租车类型费用表 - analysis_fee_by_taxi_type
        TableInfo feeByTaxiTypeTable = new TableInfo(
            "analysis_fee_by_taxi_type",
            "出租车类型费用表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("taxi_type", "text", "出租车类型"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("total_tip", "double", "总小费(美元)"),
                new ColumnInfo("avg_tip", "double", "平均小费(美元)"),
                new ColumnInfo("tip_rate", "double", "小费率"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 13. 距离分布表 - analysis_distance_distribution
        TableInfo distanceDistributionTable = new TableInfo(
            "analysis_distance_distribution",
            "距离分布分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("distance_range", "text", "距离区间(英里)"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("avg_distance", "double", "平均距离(英里)"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 14. 时长分布表 - analysis_duration_distribution
        TableInfo durationDistributionTable = new TableInfo(
            "analysis_duration_distribution",
            "时长分布分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("duration_range", "text", "时长区间(分钟)"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("avg_duration", "double", "平均时长(分钟)"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 15. 乘客分布表 - analysis_passenger_distribution
        TableInfo passengerDistributionTable = new TableInfo(
            "analysis_passenger_distribution",
            "乘客人数分布表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("passenger_count", "int", "乘客人数"),
                new ColumnInfo("passenger_range", "text", "乘客范围描述"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 16. 收入贡献表 - analysis_revenue_contribution
        TableInfo revenueContributionTable = new TableInfo(
            "analysis_revenue_contribution",
            "收入贡献分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("pu_zone", "text", "上车区域"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("revenue_ratio", "double", "收入占比"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 17. 机场分析表 - analysis_airport
        TableInfo airportTable = new TableInfo(
            "analysis_airport",
            "机场订单分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("airport_trip", "text", "机场类型"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("trip_ratio", "double", "订单占比"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 18. 供应商分析表 - analysis_vendor
        TableInfo vendorTable = new TableInfo(
            "analysis_vendor",
            "供应商分析表",
            Arrays.asList(
                new ColumnInfo("stat_date", "date", "统计日期"),
                new ColumnInfo("vendor_name", "text", "供应商名称"),
                new ColumnInfo("trip_count", "double", "订单数"),
                new ColumnInfo("total_revenue", "double", "总收入(美元)"),
                new ColumnInfo("avg_fare", "double", "平均车费(美元)"),
                new ColumnInfo("avg_distance", "double", "平均距离(英里)"),
                new ColumnInfo("revenue_ratio", "double", "收入占比"),
                new ColumnInfo("update_time", "text", "更新时间")
            )
        );

        // 将所有表添加到schema
        SchemaInfo schemaInfo = new SchemaInfo(
            "nyc_taxi_ads",
            Arrays.asList(
                kpiDailyTable,
                hourlyDistributionTable,
                weekdayAnalysisTable,
                paymentAnalysisTable,
                pickupHotspotsTable,
                dropoffHotspotsTable,
                boroughFlowTable,
                feeCompositionTable,
                feePercentageTable,
                feeByBoroughTable,
                feeTrendTable,
                feeByTaxiTypeTable,
                distanceDistributionTable,
                durationDistributionTable,
                passengerDistributionTable,
                revenueContributionTable,
                airportTable,
                vendorTable
            )
        );

        schemaCache.put("nyc_taxi_ads", schemaInfo);
        schemaCache.put("taxi", schemaInfo);  // 别名
        schemaCache.put("default", schemaInfo);  // 默认
        log.info("已初始化 {} 张ADS层表结构 (基于 ADS_Table_Schema1.2.md)", schemaInfo.getTables().size());
    }

    /**
     * 获取数据库schema信息
     */
    public Map<String, Object> getSchema(String database) {
        SchemaInfo schemaInfo = schemaCache.getOrDefault(database, schemaCache.get("default"));
        Map<String, Object> result = new HashMap<>();
        if (schemaInfo != null) {
            result.put("database", schemaInfo.getDatabase());
            result.put("databaseComment", "纽约出租车ADS层分析表");
            result.put("dataTimeRange", "2025年1月1日 至 2025年3月31日");
            List<Map<String, Object>> tables = new ArrayList<>();
            for (TableInfo table : schemaInfo.getTables()) {
                Map<String, Object> tableMap = new HashMap<>();
                tableMap.put("tableName", table.getTableName());
                tableMap.put("tableComment", table.getTableComment());
                List<Map<String, Object>> columns = new ArrayList<>();
                for (ColumnInfo column : table.getColumns()) {
                    Map<String, Object> columnMap = new HashMap<>();
                    columnMap.put("name", column.getName());
                    columnMap.put("type", column.getType());
                    columnMap.put("comment", column.getComment());
                    columns.add(columnMap);
                }
                tableMap.put("columns", columns);
                tables.add(tableMap);
            }
            result.put("tables", tables);
        }
        return result;
    }

    /**
     * 获取单个表的schema信息
     */
    public TableInfo getTableSchema(String tableName) {
        for (SchemaInfo schemaInfo : schemaCache.values()) {
            for (TableInfo table : schemaInfo.getTables()) {
                if (table.getTableName().equalsIgnoreCase(tableName)) {
                    return table;
                }
            }
        }
        return null;
    }

    /**
     * 获取所有表名列表
     */
    public List<String> getAllTableNames() {
        SchemaInfo schemaInfo = schemaCache.get("default");
        if (schemaInfo != null) {
            return schemaInfo.getTables().stream()
                .map(TableInfo::getTableName)
                .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 获取所有可用的指标字段
     */
    public List<String> getAllMetrics() {
        Set<String> metrics = new HashSet<>();
        SchemaInfo schemaInfo = schemaCache.get("default");
        if (schemaInfo != null) {
            for (TableInfo table : schemaInfo.getTables()) {
                for (ColumnInfo column : table.getColumns()) {
                    String name = column.getName().toLowerCase();
                    if (name.contains("count") || name.contains("revenue") || 
                        name.contains("fare") || name.contains("tip") || 
                        name.contains("distance") || name.contains("duration") ||
                        name.contains("amount") || name.contains("percentage") ||
                        name.contains("ratio") || name.contains("growth") ||
                        name.contains("rate")) {
                        metrics.add(column.getName());
                    }
                }
            }
        }
        return new ArrayList<>(metrics);
    }

    /**
     * 获取所有可用的维度字段
     */
    public List<String> getAllDimensions() {
        Set<String> dimensions = new HashSet<>();
        SchemaInfo schemaInfo = schemaCache.get("default");
        if (schemaInfo != null) {
            for (TableInfo table : schemaInfo.getTables()) {
                for (ColumnInfo column : table.getColumns()) {
                    String name = column.getName().toLowerCase();
                    if (name.equals("stat_date") || name.equals("date") ||
                        name.contains("hour") || name.contains("weekday") ||
                        name.contains("zone") || name.contains("borough") ||
                        name.contains("payment") || name.contains("vendor") ||
                        name.contains("type") || name.contains("range") ||
                        name.contains("passenger") || name.contains("airport") ||
                        name.contains("name") || name.contains("code")) {
                        dimensions.add(column.getName());
                    }
                }
            }
        }
        return new ArrayList<>(dimensions);
    }

    /**
     * 刷新schema缓存
     */
    public void refreshSchema(String database) {
        log.info("刷新schema缓存，数据库: {}", database);
        initializeSchema();
    }

    /**
     * Schema信息内部类
     */
    static class SchemaInfo {
        private String database;
        private List<TableInfo> tables;

        public SchemaInfo(String database, List<TableInfo> tables) {
            this.database = database;
            this.tables = tables;
        }

        public String getDatabase() { return database; }
        public void setDatabase(String database) { this.database = database; }
        public List<TableInfo> getTables() { return tables; }
        public void setTables(List<TableInfo> tables) { this.tables = tables; }
    }

    /**
     * 表信息内部类
     */
    public static class TableInfo {
        private String tableName;
        private String tableComment;
        private List<ColumnInfo> columns;

        public TableInfo(String tableName, String tableComment, List<ColumnInfo> columns) {
            this.tableName = tableName;
            this.tableComment = tableComment;
            this.columns = columns;
        }

        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getTableComment() { return tableComment; }
        public void setTableComment(String tableComment) { this.tableComment = tableComment; }
        public List<ColumnInfo> getColumns() { return columns; }
        public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }
    }

    /**
     * 列信息内部类
     */
    public static class ColumnInfo {
        private String name;
        private String type;
        private String comment;

        public ColumnInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}

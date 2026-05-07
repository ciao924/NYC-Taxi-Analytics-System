package com.taxi.analytics.modules.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaRetriever {

    private static final Logger log = LoggerFactory.getLogger(SchemaRetriever.class);

    private final Map<String, SchemaInfo> schemaCache = new HashMap<>();

    public SchemaRetriever() {
        initializeSchema();
    }

    private void initializeSchema() {
        TableInfo taxiTripTable = new TableInfo(
            "dwd_taxi_trip",
            Arrays.asList(
                new ColumnInfo("trip_id", "string", "行程ID"),
                new ColumnInfo("vendor_id", "int", "供应商ID"),
                new ColumnInfo("pickup_datetime", "timestamp", "上车时间"),
                new ColumnInfo("dropoff_datetime", "timestamp", "下车时间"),
                new ColumnInfo("passenger_count", "int", "乘客数量"),
                new ColumnInfo("trip_distance", "double", "行程距离"),
                new ColumnInfo("pickup_location_id", "int", "上车位置ID"),
                new ColumnInfo("dropoff_location_id", "int", "下车位置ID"),
                new ColumnInfo("payment_type", "int", "支付方式"),
                new ColumnInfo("fare_amount", "double", "票价"),
                new ColumnInfo("extra", "double", "附加费"),
                new ColumnInfo("tip_amount", "double", "小费"),
                new ColumnInfo("tolls_amount", "double", "过路费"),
                new ColumnInfo("improvement_surcharge", "double", "改善附加费"),
                new ColumnInfo("total_amount", "double", "总金额"),
                new ColumnInfo("dt", "string", "分区日期")
            )
        );

        TableInfo qualityMetricsTable = new TableInfo(
            "dwd_quality_metrics",
            Arrays.asList(
                new ColumnInfo("metric_id", "string", "指标ID"),
                new ColumnInfo("table_name", "string", "表名"),
                new ColumnInfo("metric_type", "string", "指标类型"),
                new ColumnInfo("metric_value", "double", "指标值"),
                new ColumnInfo("threshold", "double", "阈值"),
                new ColumnInfo("status", "string", "状态"),
                new ColumnInfo("dt", "string", "分区日期")
            )
        );

        TableInfo realtimeStatsTable = new TableInfo(
            "realtime_hourly_stats",
            Arrays.asList(
                new ColumnInfo("hour_key", "string", "小时键"),
                new ColumnInfo("trip_count", "bigint", "行程数量"),
                new ColumnInfo("avg_fare", "double", "平均票价"),
                new ColumnInfo("total_amount", "double", "总金额"),
                new ColumnInfo("update_time", "timestamp", "更新时间")
            )
        );

        SchemaInfo schemaInfo = new SchemaInfo(
            "taxi",
            Arrays.asList(taxiTripTable, qualityMetricsTable, realtimeStatsTable)
        );

        schemaCache.put("taxi", schemaInfo);
    }

    public Map<String, Object> getSchema(String database) {
        SchemaInfo schemaInfo = schemaCache.getOrDefault(database, schemaCache.get("taxi"));
        // 转换为Map格式返回
        Map<String, Object> result = new HashMap<>();
        if (schemaInfo != null) {
            result.put("database", schemaInfo.getDatabase());
            List<Map<String, Object>> tables = new ArrayList<>();
            for (TableInfo table : schemaInfo.getTables()) {
                Map<String, Object> tableMap = new HashMap<>();
                tableMap.put("tableName", table.getTableName());
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

    public TableInfo getTableSchema(String tableName) {
        SchemaInfo schemaInfo = schemaCache.get("taxi");
        if (schemaInfo != null) {
            for (TableInfo table : schemaInfo.getTables()) {
                if (table.getTableName().equals(tableName)) {
                    return table;
                }
            }
        }
        return null;
    }

    public void refreshSchema(String database) {
        log.info("Refreshing schema for database: {}", database);
        initializeSchema();
    }
}

class SchemaInfo {
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

class TableInfo {
    private String tableName;
    private List<ColumnInfo> columns;

    public TableInfo(String tableName, List<ColumnInfo> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public List<ColumnInfo> getColumns() { return columns; }
    public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }
}

class ColumnInfo {
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

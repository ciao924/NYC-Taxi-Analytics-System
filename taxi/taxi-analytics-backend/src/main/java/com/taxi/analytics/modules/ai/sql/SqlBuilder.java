package com.taxi.analytics.modules.ai.sql;

import com.taxi.analytics.modules.ai.dsl.Dsl;
import com.taxi.analytics.modules.ai.metrics.MetricDef;
import com.taxi.analytics.modules.ai.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(SqlBuilder.class);
    
    @Autowired
    private MetricRegistry metricRegistry;

    public String build(Dsl dsl) {
        log.info("开始构建 SQL，DSL：{}", dsl);

        MetricDef metricDef = metricRegistry.getMetricDef(dsl.getMetric());

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        buildSelectClause(sql, dsl, metricDef);
        sql.append(" FROM " + metricDef.getTableName());

        buildWhereClause(sql, dsl);
        buildGroupByClause(sql, dsl);
        buildOrderByClause(sql, dsl);

        String finalSql = sql.toString();
        log.info("生成的 SQL：{}", finalSql);
        return finalSql;
    }

    private void buildSelectClause(StringBuilder sql, Dsl dsl, MetricDef metricDef) {
        String aggregation = metricDef.getAggregation();
        String fieldName = metricDef.getFieldName();

        if ("AVG".equals(aggregation)) {
            sql.append("AVG(" + fieldName + ") AS value");
        } else if ("SUM".equals(aggregation)) {
            sql.append("SUM(" + fieldName + ") AS value");
        } else if ("COUNT".equals(aggregation)) {
            sql.append("COUNT(" + fieldName + ") AS value");
        } else {
            sql.append(fieldName + " AS value");
        }

        if (dsl.getDimension() != null && !dsl.getDimension().isEmpty()) {
            sql.append(", " + dsl.getDimension() + " AS dimension");
        }
    }

    private void buildWhereClause(StringBuilder sql, Dsl dsl) {
        boolean hasCondition = false;

        if (dsl.getTimeRange() != null && !dsl.getTimeRange().isEmpty()) {
            sql.append(" WHERE date >= CURRENT_DATE - INTERVAL '" + dsl.getTimeRange() + "'");
            hasCondition = true;
        }

        if (dsl.getFilters() != null && !dsl.getFilters().isEmpty()) {
            if (!hasCondition) {
                sql.append(" WHERE ");
            } else {
                sql.append(" AND ");
            }

            buildFilterConditions(sql, dsl.getFilters());
        }
    }

    private void buildFilterConditions(StringBuilder sql, Map<String, Object> filters) {
        boolean first = true;
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (!first) {
                sql.append(" AND ");
            }
            first = false;

            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.endsWith("_start") || key.endsWith("_min")) {
                String fieldName = key.replace("_start", "").replace("_min", "");
                sql.append(fieldName + " >= ?");
            } else if (key.endsWith("_end") || key.endsWith("_max")) {
                String fieldName = key.replace("_end", "").replace("_max", "");
                sql.append(fieldName + " <= ?");
            } else {
                sql.append(key + " = ?");
            }
        }
    }

    private void buildGroupByClause(StringBuilder sql, Dsl dsl) {
        if (dsl.getDimension() != null && !dsl.getDimension().isEmpty()) {
            sql.append(" GROUP BY " + dsl.getDimension());
        }
    }

    private void buildOrderByClause(StringBuilder sql, Dsl dsl) {
        if (dsl.getDimension() != null && !dsl.getDimension().isEmpty()) {
            sql.append(" ORDER BY " + dsl.getDimension());
        } else {
            sql.append(" ORDER BY date DESC");
        }
    }
}
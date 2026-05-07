package com.taxi.analytics.modules.ai.guard;

import com.taxi.analytics.modules.ai.dsl.Dsl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;

@Component
public class QueryGuard {

    private static final Logger log = LoggerFactory.getLogger(QueryGuard.class);
    
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("^(\\d+)d$");
    private static final Set<String> ALLOWED_TABLES = new HashSet<>();
    
    static {
        ALLOWED_TABLES.add("dws_taxi_day");
    }

    private static final int MAX_TIME_RANGE_DAYS = 30;
    private static final int MAX_GROUP_BY_DIMENSIONS = 2;
    private static final int MAX_RETURN_ROWS = 1000;

    public void check(Dsl dsl) {
        log.info("开始查询防护检查，DSL：{}", dsl);

        checkTimeRange(dsl.getTimeRange());
        checkGroupByDimensions(dsl.getDimension());
        checkTableAccess(dsl);

        log.info("查询防护检查通过");
    }

    private void checkTimeRange(String timeRange) {
        if (timeRange == null || timeRange.isEmpty()) {
            return;
        }

        Matcher matcher = TIME_RANGE_PATTERN.matcher(timeRange);
        if (!matcher.matches()) {
            throw new RuntimeException("时间范围格式错误");
        }

        int days = Integer.parseInt(matcher.group(1));
        if (days > MAX_TIME_RANGE_DAYS) {
            throw new RuntimeException("时间范围超过限制，最大允许 " + MAX_TIME_RANGE_DAYS + " 天");
        }
    }

    private void checkGroupByDimensions(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            return;
        }

        String[] dimensions = dimension.split(",");
        if (dimensions.length > MAX_GROUP_BY_DIMENSIONS) {
            throw new RuntimeException("分组维度超过限制，最大允许 " + MAX_GROUP_BY_DIMENSIONS + " 个维度");
        }
    }

    private void checkTableAccess(Dsl dsl) {
        String metric = dsl.getMetric();
        if (metric == null || metric.isEmpty()) {
            return;
        }

        if (!ALLOWED_TABLES.contains("dws_taxi_day")) {
            throw new RuntimeException("禁止访问非 DWS 层表");
        }
    }

    public int getMaxReturnRows() {
        return MAX_RETURN_ROWS;
    }

    public int getMaxTimeRangeDays() {
        return MAX_TIME_RANGE_DAYS;
    }

    public int getMaxGroupByDimensions() {
        return MAX_GROUP_BY_DIMENSIONS;
    }
}
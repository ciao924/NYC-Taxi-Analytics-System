package com.taxi.analytics.modules.ai.dsl;

import com.taxi.analytics.modules.ai.service.SchemaRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DslConverter - 自然语言到DSL转换服务
 * 实现业务员提需求 -> 智能体转换为DSL的完整流程
 */
@Component
public class DslConverter {

    private static final Logger log = LoggerFactory.getLogger(DslConverter.class);

    private final SchemaRetriever schemaRetriever;
    private final DslValidator dslValidator;

    // 时间模式匹配
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-/年](\\d{1,2})[-/月](\\d{1,2})日?");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})\\s*[至到-]\\s*(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})");
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\s*天");
    private static final Pattern RECENT_PATTERN = Pattern.compile("最近\\s*(\\d+)\\s*(天|周|个月?)");

    // 指标映射 - 支持更多业务指标
    private static final Map<String, List<String>> METRIC_MAPPINGS = new HashMap<>();
    // 维度映射 - 支持更多维度
    private static final Map<String, List<String>> DIMENSION_MAPPINGS = new HashMap<>();
    // 图表类型映射
    private static final Map<String, String> CHART_TYPE_MAPPINGS = new HashMap<>();
    // 常用时间词汇映射
    private static final Map<String, String> TIME_RANGE_MAPPINGS = new HashMap<>();

    static {
        // ========== 指标映射（增强版）==========
        // 订单相关
        METRIC_MAPPINGS.put("订单", Arrays.asList("total_trips", "trip_count"));
        METRIC_MAPPINGS.put("订单数", Arrays.asList("total_trips", "trip_count"));
        METRIC_MAPPINGS.put("总订单", Arrays.asList("total_trips"));
        METRIC_MAPPINGS.put("订单总量", Arrays.asList("total_trips"));
        METRIC_MAPPINGS.put("上车次数", Arrays.asList("pickup_count"));
        METRIC_MAPPINGS.put("下车次数", Arrays.asList("dropoff_count"));
        
        // 收入相关
        METRIC_MAPPINGS.put("收入", Arrays.asList("total_revenue", "total_amount"));
        METRIC_MAPPINGS.put("总收入", Arrays.asList("total_revenue"));
        METRIC_MAPPINGS.put("总金额", Arrays.asList("total_amount"));
        METRIC_MAPPINGS.put("金额", Arrays.asList("total_amount"));
        
        // 费用相关
        METRIC_MAPPINGS.put("车费", Arrays.asList("total_fare", "avg_fare"));
        METRIC_MAPPINGS.put("总车费", Arrays.asList("total_fare"));
        METRIC_MAPPINGS.put("平均车费", Arrays.asList("avg_fare"));
        
        // 小费相关
        METRIC_MAPPINGS.put("小费", Arrays.asList("total_tip", "avg_tip"));
        METRIC_MAPPINGS.put("总小费", Arrays.asList("total_tip"));
        METRIC_MAPPINGS.put("平均小费", Arrays.asList("avg_tip"));
        
        // 距离相关
        METRIC_MAPPINGS.put("距离", Arrays.asList("avg_distance"));
        METRIC_MAPPINGS.put("平均距离", Arrays.asList("avg_distance"));
        METRIC_MAPPINGS.put("里程", Arrays.asList("avg_distance"));
        
        // 时长相关
        METRIC_MAPPINGS.put("时长", Arrays.asList("avg_duration"));
        METRIC_MAPPINGS.put("平均时长", Arrays.asList("avg_duration"));
        METRIC_MAPPINGS.put("时间", Arrays.asList("avg_duration"));
        
        // 占比/比率相关
        METRIC_MAPPINGS.put("占比", Arrays.asList("percentage", "revenue_ratio"));
        METRIC_MAPPINGS.put("比率", Arrays.asList("revenue_ratio"));
        METRIC_MAPPINGS.put("比例", Arrays.asList("percentage"));
        METRIC_MAPPINGS.put("贡献", Arrays.asList("revenue_ratio"));
        
        // 趋势相关
        METRIC_MAPPINGS.put("趋势", Arrays.asList("total_trips", "total_revenue"));
        METRIC_MAPPINGS.put("增长", Arrays.asList("total_trips", "total_revenue"));
        METRIC_MAPPINGS.put("变化", Arrays.asList("total_trips", "total_revenue"));

        // ========== 维度映射（增强版）==========
        DIMENSION_MAPPINGS.put("日期", Arrays.asList("stat_date"));
        DIMENSION_MAPPINGS.put("时间", Arrays.asList("stat_date", "hour_of_day"));
        DIMENSION_MAPPINGS.put("小时", Arrays.asList("hour_of_day"));
        DIMENSION_MAPPINGS.put("时段", Arrays.asList("hour_of_day"));
        DIMENSION_MAPPINGS.put("星期", Arrays.asList("day_of_week", "day_name"));
        DIMENSION_MAPPINGS.put("周", Arrays.asList("day_of_week"));
        DIMENSION_MAPPINGS.put("工作日", Arrays.asList("day_of_week"));
        
        // 区域相关
        DIMENSION_MAPPINGS.put("区域", Arrays.asList("pu_zone", "do_zone", "borough"));
        DIMENSION_MAPPINGS.put("行政区", Arrays.asList("pu_borough", "do_borough", "borough"));
        DIMENSION_MAPPINGS.put("上车区域", Arrays.asList("pu_zone"));
        DIMENSION_MAPPINGS.put("下车区域", Arrays.asList("do_zone"));
        DIMENSION_MAPPINGS.put("上车行政区", Arrays.asList("pu_borough"));
        DIMENSION_MAPPINGS.put("下车行政区", Arrays.asList("do_borough"));
        
        // 分类相关
        DIMENSION_MAPPINGS.put("支付", Arrays.asList("payment_name"));
        DIMENSION_MAPPINGS.put("支付方式", Arrays.asList("payment_name"));
        DIMENSION_MAPPINGS.put("供应商", Arrays.asList("vendor_name"));
        DIMENSION_MAPPINGS.put("公司", Arrays.asList("vendor_name"));
        DIMENSION_MAPPINGS.put("类型", Arrays.asList("taxi_type"));
        DIMENSION_MAPPINGS.put("出租车类型", Arrays.asList("taxi_type"));
        DIMENSION_MAPPINGS.put("机场", Arrays.asList("airport_trip"));
        DIMENSION_MAPPINGS.put("机场类型", Arrays.asList("airport_trip"));
        
        // 分布相关
        DIMENSION_MAPPINGS.put("乘客", Arrays.asList("passenger_count"));
        DIMENSION_MAPPINGS.put("乘客人数", Arrays.asList("passenger_count"));
        DIMENSION_MAPPINGS.put("距离区间", Arrays.asList("distance_range"));
        DIMENSION_MAPPINGS.put("时长区间", Arrays.asList("duration_range"));

        // ========== 图表类型映射（增强版）==========
        CHART_TYPE_MAPPINGS.put("趋势", "line");
        CHART_TYPE_MAPPINGS.put("走势", "line");
        CHART_TYPE_MAPPINGS.put("变化", "line");
        CHART_TYPE_MAPPINGS.put("增长", "line");
        CHART_TYPE_MAPPINGS.put("排行", "bar");
        CHART_TYPE_MAPPINGS.put("排名", "bar");
        CHART_TYPE_MAPPINGS.put("TOP", "bar");
        CHART_TYPE_MAPPINGS.put("对比", "bar");
        CHART_TYPE_MAPPINGS.put("比较", "bar");
        CHART_TYPE_MAPPINGS.put("占比", "pie");
        CHART_TYPE_MAPPINGS.put("比例", "pie");
        CHART_TYPE_MAPPINGS.put("构成", "pie");
        CHART_TYPE_MAPPINGS.put("分布", "histogram");
        CHART_TYPE_MAPPINGS.put("散点", "scatter");
        
        // ========== 时间范围快捷映射 ==========
        TIME_RANGE_MAPPINGS.put("今天", "1d");
        TIME_RANGE_MAPPINGS.put("昨日", "1d");
        TIME_RANGE_MAPPINGS.put("昨天", "1d");
        TIME_RANGE_MAPPINGS.put("本周", "7d");
        TIME_RANGE_MAPPINGS.put("近7天", "7d");
        TIME_RANGE_MAPPINGS.put("最近7天", "7d");
        TIME_RANGE_MAPPINGS.put("近一周", "7d");
        TIME_RANGE_MAPPINGS.put("近30天", "30d");
        TIME_RANGE_MAPPINGS.put("最近30天", "30d");
        TIME_RANGE_MAPPINGS.put("近一个月", "30d");
        TIME_RANGE_MAPPINGS.put("本月", "30d");
        TIME_RANGE_MAPPINGS.put("上月", "-30d,-1d");
        TIME_RANGE_MAPPINGS.put("上周", "-7d,-1d");
        TIME_RANGE_MAPPINGS.put("近一年", "365d");
        TIME_RANGE_MAPPINGS.put("最近一年", "365d");
    }

    public DslConverter(SchemaRetriever schemaRetriever, DslValidator dslValidator) {
        this.schemaRetriever = schemaRetriever;
        this.dslValidator = dslValidator;
    }

    /**
     * 将自然语言查询转换为DSL对象
     *
     * @param query 自然语言查询
     * @return DSL对象
     */
    public Dsl convert(String query) {
        log.info("开始将自然语言转换为DSL，查询: {}", query);
        
        Dsl dsl = new Dsl();
        
        // 1. 解析指标
        String metric = parseMetric(query);
        dsl.setMetric(metric);
        
        // 2. 解析时间范围
        String timeRange = parseTimeRange(query);
        dsl.setTimeRange(timeRange);
        
        // 3. 解析维度
        String dimension = parseDimension(query);
        dsl.setDimension(dimension);
        
        // 4. 解析过滤器
        Map<String, Object> filters = parseFilters(query);
        dsl.setFilters(filters);
        
        // 5. 推荐图表类型
        String chartType = recommendChartType(query, metric, dimension);
        dsl.setChart(chartType);
        
        log.info("DSL转换完成: metric={}, timeRange={}, dimension={}, chart={}", 
                metric, timeRange, dimension, chartType);
        
        return dsl;
    }

    /**
     * 解析指标字段
     */
    private String parseMetric(String query) {
        for (Map.Entry<String, List<String>> entry : METRIC_MAPPINGS.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue().get(0);
            }
        }
        // 默认返回订单数
        return "total_trips";
    }

    /**
     * 解析时间范围（增强版）
     */
    private String parseTimeRange(String query) {
        // 1. 优先检查快捷时间词汇映射
        for (Map.Entry<String, String> entry : TIME_RANGE_MAPPINGS.entrySet()) {
            if (query.contains(entry.getKey())) {
                // 特殊处理"今天"和"昨天"，返回具体日期
                if ("今天".equals(entry.getKey())) {
                    return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                } else if ("昨天".equals(entry.getKey()) || "昨日".equals(entry.getKey())) {
                    return LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                return entry.getValue();
            }
        }

        // 2. 检查是否有具体日期范围 "YYYY-MM-DD 至 YYYY-MM-DD"
        Matcher rangeMatcher = DATE_RANGE_PATTERN.matcher(query);
        if (rangeMatcher.find()) {
            return rangeMatcher.group(1) + "," + rangeMatcher.group(2);
        }

        // 3. 检查是否有具体日期
        Matcher dateMatcher = DATE_PATTERN.matcher(query);
        if (dateMatcher.find()) {
            return dateMatcher.group();
        }

        // 4. 检查是否有"最近X天/周/月"模式
        Matcher recentMatcher = RECENT_PATTERN.matcher(query);
        if (recentMatcher.find()) {
            int num = Integer.parseInt(recentMatcher.group(1));
            String unit = recentMatcher.group(2);
            
            if ("周".equals(unit)) {
                return (num * 7) + "d";
            } else if ("个月".equals(unit) || "月".equals(unit)) {
                return (num * 30) + "d";
            } else {
                return num + "d";
            }
        }

        // 5. 检查是否有"X天"模式
        Matcher daysMatcher = DAYS_PATTERN.matcher(query);
        if (daysMatcher.find()) {
            return daysMatcher.group(1) + "d";
        }

        // 默认返回最近7天
        return "7d";
    }

    /**
     * 解析维度字段（增强版）
     */
    private String parseDimension(String query) {
        // 1. 优先使用SchemaRetriever获取可用维度，进行智能匹配
        List<String> availableDimensions = schemaRetriever.getAllDimensions();
        
        // 2. 建立维度到中文名称的映射
        Map<String, String> dimensionToChinese = new HashMap<>();
        dimensionToChinese.put("stat_date", "日期");
        dimensionToChinese.put("hour_of_day", "小时");
        dimensionToChinese.put("day_of_week", "星期");
        dimensionToChinese.put("day_name", "星期");
        dimensionToChinese.put("payment_name", "支付");
        dimensionToChinese.put("vendor_name", "供应商");
        dimensionToChinese.put("taxi_type", "类型");
        dimensionToChinese.put("borough", "行政区");
        dimensionToChinese.put("pu_borough", "上车");
        dimensionToChinese.put("do_borough", "下车");
        dimensionToChinese.put("pu_zone", "上车区域");
        dimensionToChinese.put("do_zone", "下车区域");
        dimensionToChinese.put("passenger_count", "乘客");
        dimensionToChinese.put("distance_range", "距离");
        dimensionToChinese.put("duration_range", "时长");
        dimensionToChinese.put("airport_trip", "机场");
        
        // 3. 按优先级匹配维度
        List<String> priorityOrder = Arrays.asList(
            "pu_zone", "do_zone", "borough", "pu_borough", "do_borough",
            "payment_name", "vendor_name", "taxi_type",
            "hour_of_day", "day_of_week", "day_name",
            "passenger_count", "distance_range", "duration_range", "airport_trip"
        );
        
        for (String dim : priorityOrder) {
            if (availableDimensions.contains(dim)) {
                String dimCn = dimensionToChinese.get(dim);
                if (dimCn != null && query.contains(dimCn)) {
                    log.debug("匹配到维度: {} ({})", dim, dimCn);
                    return dim;
                }
            }
        }
        
        // 4. 特殊关键词匹配
        if (query.contains("排名") || query.contains("排行") || query.contains("TOP") || query.contains("热点")) {
            return "pu_zone";
        }
        
        if (query.contains("趋势") || query.contains("每天") || query.contains("每日") || 
            query.contains("变化") || query.contains("增长")) {
            return "stat_date";
        }
        
        if (query.contains("对比") || query.contains("比较")) {
            // 默认使用供应商进行对比
            return "vendor_name";
        }
        
        // 默认返回日期维度
        return "stat_date";
    }

    /**
     * 获取维度的中文名称
     */
    private String getDimensionChinese(String dimension) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("stat_date", "日期");
        mapping.put("hour_of_day", "小时");
        mapping.put("day_of_week", "星期");
        mapping.put("payment_name", "支付");
        mapping.put("vendor_name", "供应商");
        mapping.put("taxi_type", "类型");
        mapping.put("borough", "行政区");
        mapping.put("pu_zone", "上车");
        mapping.put("do_zone", "下车");
        mapping.put("pu_borough", "上车");
        mapping.put("do_borough", "下车");
        mapping.put("passenger_count", "乘客");
        mapping.put("distance_range", "距离");
        mapping.put("duration_range", "时长");
        mapping.put("airport_trip", "机场");
        return mapping.get(dimension);
    }

    /**
     * 解析过滤器条件
     */
    private Map<String, Object> parseFilters(String query) {
        Map<String, Object> filters = new HashMap<>();
        
        // 解析出租车类型过滤
        if (query.contains("黄车") || query.contains("黄色")) {
            filters.put("taxi_type", "yellow");
        } else if (query.contains("绿车") || query.contains("绿色")) {
            filters.put("taxi_type", "green");
        }
        
        // 解析行政区过滤
        List<String> boroughs = Arrays.asList("曼哈顿", "布鲁克林", "皇后区", "布朗克斯", "史泰登岛");
        for (String borough : boroughs) {
            if (query.contains(borough)) {
                filters.put("borough", translateBorough(borough));
                break;
            }
        }
        
        // 解析支付方式过滤
        if (query.contains("现金")) {
            filters.put("payment_type", "cash");
        } else if (query.contains("信用卡") || query.contains("刷卡")) {
            filters.put("payment_type", "credit");
        }
        
        return filters;
    }

    /**
     * 行政区中文转英文
     */
    private String translateBorough(String boroughCn) {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("曼哈顿", "Manhattan");
        mapping.put("布鲁克林", "Brooklyn");
        mapping.put("皇后区", "Queens");
        mapping.put("布朗克斯", "Bronx");
        mapping.put("史泰登岛", "Staten Island");
        return mapping.getOrDefault(boroughCn, boroughCn);
    }

    /**
     * 根据查询内容推荐图表类型
     */
    private String recommendChartType(String query, String metric, String dimension) {
        // 根据查询中的关键词推荐
        for (Map.Entry<String, String> entry : CHART_TYPE_MAPPINGS.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 根据维度类型推荐
        if ("stat_date".equals(dimension)) {
            return "line";
        } else if ("pu_zone".equals(dimension) || "borough".equals(dimension) || 
                   "payment_name".equals(dimension) || "vendor_name".equals(dimension)) {
            return "bar";
        } else if ("taxi_type".equals(dimension) || "passenger_count".equals(dimension)) {
            return "pie";
        }
        
        // 默认使用柱状图
        return "bar";
    }

    /**
     * 验证并优化DSL
     * 严格遵循规范：不进行自动降级，如果DSL无效则抛出异常
     */
    public Dsl validateAndOptimize(Dsl dsl) {
        log.info("验证并优化DSL: {}", dsl);
        
        try {
            dslValidator.validate(dsl);
            log.info("DSL验证通过");
        } catch (Exception e) {
            log.error("DSL验证失败，不进行自动降级: {}", e.getMessage());
            // 不再自动优化，直接抛出异常
            throw new RuntimeException("DSL验证失败: " + e.getMessage(), e);
        }
        
        return dsl;
    }

    /**
     * 获取DSL的描述信息
     */
    public String describeDsl(Dsl dsl) {
        StringBuilder sb = new StringBuilder();
        sb.append("查询解析结果:\n");
        sb.append("  - 指标: ").append(dsl.getMetric()).append("\n");
        sb.append("  - 维度: ").append(dsl.getDimension()).append("\n");
        sb.append("  - 时间范围: ").append(dsl.getTimeRange()).append("\n");
        sb.append("  - 图表类型: ").append(dsl.getChart()).append("\n");
        
        if (dsl.getFilters() != null && !dsl.getFilters().isEmpty()) {
            sb.append("  - 过滤器: ").append(dsl.getFilters()).append("\n");
        }
        
        return sb.toString();
    }
}

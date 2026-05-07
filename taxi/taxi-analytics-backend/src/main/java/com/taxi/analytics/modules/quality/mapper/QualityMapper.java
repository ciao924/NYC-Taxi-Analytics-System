package com.taxi.analytics.modules.quality.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
@DS("quality")
public interface QualityMapper {

    /**
     * 获取质量总览评分
     */
    @Select("""
        SELECT 
            COALESCE(AVG(CASE WHEN check_status = 'PASS' THEN 100 ELSE 0 END), 0) as avg_score,
            COUNT(DISTINCT table_name) as table_count,
            COUNT(*) as check_count
        FROM data_quality_daily
        WHERE check_date = #{date}
    """)
    Map<String, Object> getQualitySummary(@Param("date") LocalDate date);

    /**
     * 获取所有表健康状态
     */
    @Select("""
        SELECT 
            table_name as table_name,
            MAX(CASE WHEN check_status = 'PASS' THEN 1 ELSE 0 END) as is_healthy,
            MAX(check_date) as last_check_date
        FROM data_quality_daily
        WHERE check_date = #{date}
        GROUP BY table_name
        ORDER BY is_healthy ASC
    """)
    List<Map<String, Object>> getTableHealthStatus(@Param("date") LocalDate date);

    /**
     * 获取完整性检测详情
     */
    @Select("""
        SELECT 
            table_name as table_name,
            check_status as completeness_status,
            expected_value as expected_rows,
            actual_value as actual_rows,
            deviation_rate as deviation_rate
        FROM data_quality_daily
        WHERE check_type = 'completeness'
            AND check_date = #{date}
        ORDER BY actual_value / NULLIF(expected_value, 0) ASC
    """)
    List<Map<String, Object>> getCompletenessDetails(@Param("date") LocalDate date);

    /**
     * 获取空值率详情
     */
    @Select("""
        SELECT 
            table_name as table_name,
            detail_json as null_rate_detail,
            check_status as null_rate_status
        FROM data_quality_daily
        WHERE check_type = 'null_rate'
            AND check_date = #{date}
        ORDER BY check_status ASC
    """)
    List<Map<String, Object>> getNullRateDetails(@Param("date") LocalDate date);

    /**
     * 获取唯一性检测详情
     */
    @Select("""
        SELECT 
            table_name as table_name,
            check_status as uniqueness_status,
            actual_value as duplicate_count,
            expected_value as expected_unique
        FROM data_quality_daily
        WHERE check_type = 'uniqueness'
            AND check_date = #{date}
        ORDER BY check_status ASC
    """)
    List<Map<String, Object>> getUniquenessDetails(@Param("date") LocalDate date);

    /**
     * 获取一致性检测详情
     */
    @Select("""
        SELECT 
            table_name as table_name,
            check_status as consistency_status,
            detail_json as consistency_detail,
            deviation_rate as deviation_rate
        FROM data_quality_daily
        WHERE check_type = 'consistency'
            AND check_date = #{date}
        ORDER BY check_status ASC
    """)
    List<Map<String, Object>> getConsistencyDetails(@Param("date") LocalDate date);

    /**
     * 获取范围检测详情
     */
    @Select("""
        SELECT 
            table_name as table_name,
            check_status as range_status,
            detail_json as range_detail
        FROM data_quality_daily
        WHERE check_type = 'range'
            AND check_date = #{date}
        ORDER BY check_status ASC
    """)
    List<Map<String, Object>> getRangeDetails(@Param("date") LocalDate date);

    /**
     * 获取数据及时性详情
     */
    @Select("""
        SELECT 
            table_name as table_name,
            check_status as freshness_status,
            actual_value as delay_hours
        FROM data_quality_daily
        WHERE check_type = 'freshness'
            AND check_date = #{date}
        ORDER BY actual_value DESC
    """)
    List<Map<String, Object>> getFreshnessDetails(@Param("date") LocalDate date);

    /**
     * 获取告警列表
     */
    @Select("""
        SELECT 
            id,
            alert_level as alert_level,
            alert_content as alert_content,
            table_name as table_name,
            check_type as check_type,
            actual_value as actual_value,
            threshold_value as threshold_value,
            create_time as alert_time,
            is_resolved as is_resolved,
            resolve_time as resolve_time
        FROM quality_alert_history
        WHERE DATE(create_time) >= #{startDate}
            AND DATE(create_time) <= #{endDate}
        ORDER BY create_time DESC
        LIMIT #{limit}
    """)
    List<Map<String, Object>> getAlerts(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("limit") int limit);

    /**
     * 获取质量历史趋势
     */
    @Select("""
        SELECT 
            check_date as check_date,
            SUM(CASE WHEN check_status = 'PASS' THEN 100 ELSE 0 END) * 1.0 / COUNT(*) as avg_score
        FROM data_quality_daily
        WHERE check_date >= #{startDate}
            AND check_date <= #{endDate}
        GROUP BY check_date
        ORDER BY check_date
    """)
    List<Map<String, Object>> getQualityHistory(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 获取阈值配置
     */
    @Select("""
        SELECT 
            id,
            alert_name as rule_name,
            check_type as check_type,
            table_name as table_name,
            threshold_type as threshold_type,
            warning_threshold as warning_threshold,
            critical_threshold as critical_threshold,
            enabled as enabled
        FROM quality_alert_config
        WHERE enabled = 1
        ORDER BY check_type, table_name
    """)
    List<Map<String, Object>> getThresholds();

    /**
     * 获取最新检测日期
     */
    @Select("""
        SELECT MAX(check_date) as latest_date
        FROM data_quality_daily
    """)
    LocalDate getLatestCheckDate();
}
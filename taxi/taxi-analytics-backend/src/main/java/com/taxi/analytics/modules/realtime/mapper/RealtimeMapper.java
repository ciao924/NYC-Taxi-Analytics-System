package com.taxi.analytics.modules.realtime.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
@DS("realtime")
public interface RealtimeMapper {

    @Select("""
        SELECT
            COALESCE(SUM(order_count), 0) as trip_count,
            COALESCE(SUM(total_fare), 0) as total_fare,
            COALESCE(AVG(avg_fare), 0) as avg_fare,
            MAX(window_end) as window_end
        FROM realtime_order_metrics
        ORDER BY window_end DESC
        LIMIT 1
    """)
    Map<String, Object> getLatestKpi();

    @Select("""
        SELECT
            zone,
            cnt as trip_count,
            `rank`
        FROM realtime_hotspot_topn
        ORDER BY window_end DESC, cnt DESC
        LIMIT #{limit}
    """)
    List<Map<String, Object>> getHotspotTopn(@Param("limit") int limit);

    @Select("""
        SELECT
            payment_type,
            SUM(total_amount) as total_fare,
            COUNT(*) as trip_count,
            AVG(total_amount) as avg_fare
        FROM realtime_fee_composition
        GROUP BY payment_type
        ORDER BY COUNT(*) DESC
    """)
    List<Map<String, Object>> getFeeComposition();

    @Select("""
        SELECT
            DATE_FORMAT(window_end, '%Y-%m-%d %H:00:00') as hour,
            SUM(order_count) as trip_count,
            SUM(total_fare) as total_fare
        FROM realtime_order_metrics
        WHERE window_end >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
        GROUP BY hour
        ORDER BY hour
    """)
    List<Map<String, Object>> getTrend();

    @Select("""
        SELECT 
            COALESCE(SUM(order_count), 0) as trip_count,
            COALESCE(SUM(total_fare), 0) as total_fare,
            COALESCE(AVG(avg_fare), 0) as avg_fare,
            MAX(window_end) as window_end
        FROM realtime_order_metrics
        WHERE window_end >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
          AND window_end <= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
    """)
    Map<String, Object> getCurrentWindowKpi();

    @Select("""
        SELECT 
            COALESCE(SUM(order_count), 0) as trip_count,
            COALESCE(SUM(total_fare), 0) as total_fare,
            COALESCE(AVG(avg_fare), 0) as avg_fare,
            MAX(window_end) as window_end
        FROM realtime_order_metrics
        WHERE window_end >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
          AND window_end <= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
    """)
    Map<String, Object> getPreviousWindowKpi();
}
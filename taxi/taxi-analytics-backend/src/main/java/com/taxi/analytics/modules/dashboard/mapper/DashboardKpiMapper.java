package com.taxi.analytics.modules.dashboard.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taxi.analytics.modules.dashboard.entity.DashboardKpi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
@DS("ads")
public interface DashboardKpiMapper extends BaseMapper<DashboardKpi> {
    
    // 1. 获取KPI汇总数据
    @Select("""
        SELECT 
            SUM(total_trips) as trip_count,
            SUM(total_revenue) as total_revenue,
            AVG(avg_fare) as avg_fare,
            AVG(avg_distance) as avg_distance,
            AVG(avg_duration) as avg_duration,
            SUM(airport_trips) as airport_trips
        FROM analysis_kpi_daily
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
    """)
    Map<String, Object> getKpiSummary(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate);
    
    // 2. 获取KPI趋势数据
    @Select("""
        SELECT 
            stat_date as stat_date,
            total_trips as total_trips,
            total_revenue as total_revenue,
            avg_fare as avg_fare
        FROM analysis_kpi_daily
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        ORDER BY stat_date
    """)
    List<Map<String, Object>> getKpiTrend(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    // 3. 获取小时分布数据
    @Select("""
        SELECT 
            hour_of_day as hour_of_day,
            SUM(trip_count) as trip_count,
            SUM(total_revenue) as total_revenue,
            AVG(avg_fare) as avg_fare
        FROM analysis_hourly_distribution
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY hour_of_day
        ORDER BY hour_of_day
    """)
    List<Map<String, Object>> getHourlyDistribution(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
    
    // 4. 获取星期分析数据
    @Select("""
        SELECT 
            day_of_week as day_of_week,
            day_of_week_name as day_of_week_name,
            SUM(total_trips) as total_trips,
            SUM(total_revenue) as total_revenue,
            AVG(avg_fare) as avg_fare
        FROM analysis_weekday_analysis
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY day_of_week, day_of_week_name
        ORDER BY day_of_week
    """)
    List<Map<String, Object>> getWeekdayAnalysis(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    // 5. 获取支付方式分析
    @Select("""
        SELECT 
            payment_name as payment_name,
            SUM(trip_count) as trip_count,
            SUM(total_amount) as total_amount,
            AVG(trip_ratio) as trip_ratio
        FROM analysis_payment_analysis
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY payment_name
        ORDER BY trip_count DESC
    """)
    List<Map<String, Object>> getPaymentAnalysis(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    
    // 6. 获取费用构成
    @Select("""
        SELECT 
            fee_name as fee_name,
            SUM(total_amount) as total_amount
        FROM analysis_fee_composition
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY fee_name, fee_code
        ORDER BY total_amount DESC
    """)
    List<Map<String, Object>> getFeeComposition(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
    
    // 7. 获取费用占比
    @Select("""
        SELECT 
            fee_name as fee_name,
            AVG(percentage) as percentage
        FROM analysis_fee_percentage
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY fee_name, fee_code
        ORDER BY percentage DESC
    """)
    List<Map<String, Object>> getFeePercentage(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    
    // 8. 获取上下客热点
    @Select("""
        SELECT 
            zone_name as zone_name,
            borough as borough,
            SUM(trip_count) as trip_count,
            SUM(total_revenue) as total_revenue
        FROM analysis_pickup_hotspots
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY zone_name, borough
        ORDER BY trip_count DESC
        LIMIT #{limit}
    """)
    List<Map<String, Object>> getPickupHotspots(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("limit") int limit);
    
    // 9. 获取行政区流量
    @Select("""
        SELECT 
            pu_borough as pickup_borough,
            do_borough as dropoff_borough,
            SUM(pickup_count) as trip_count
        FROM analysis_borough_flow
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY pu_borough, do_borough
        ORDER BY trip_count DESC
    """)
    List<Map<String, Object>> getBoroughFlow(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
    
    // 10. 获取可用日期范围
    @Select("""
        SELECT 
            MIN(stat_date) as min_date,
            MAX(stat_date) as max_date
        FROM analysis_kpi_daily
    """)
    Map<String, LocalDate> getAvailableDateRange();
    
    // 11. 获取供应商分析数据
    @Select("""
        SELECT 
            vendor_name as vendor_name,
            SUM(trip_count) as trip_count,
            SUM(total_revenue) as total_revenue,
            AVG(avg_fare) as avg_fare,
            AVG(avg_distance) as avg_distance
        FROM analysis_vendor
        WHERE stat_date >= #{startDate} AND stat_date <= #{endDate}
        GROUP BY vendor_name
        ORDER BY trip_count DESC
        LIMIT 10
    """)
    List<Map<String, Object>> getVendorAnalysis(@Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}
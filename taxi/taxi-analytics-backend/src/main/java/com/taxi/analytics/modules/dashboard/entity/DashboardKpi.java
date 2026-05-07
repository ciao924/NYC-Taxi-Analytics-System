package com.taxi.analytics.modules.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@TableName("analysis_kpi_daily")
public class DashboardKpi implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private Integer totalTrips;

    private Double totalRevenue;

    private Double avgFare;

    private Double avgDistance;

    private Double avgDuration;

    private Double totalTip;

    private Double avgTip;

    private Integer airportTrips;

    private Integer peakHour;

    private LocalTime updateTime;
}
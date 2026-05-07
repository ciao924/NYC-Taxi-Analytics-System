package com.taxi.analytics.modules.map.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MapHeatmapMapper {

    /**
     * 查询上车点热力图数据
     */
    List<Map<String, Object>> selectPickupHeatmap(@Param("date") String date, @Param("dataSource") String dataSource, @Param("limit") int limit);

    /**
     * 查询下车点热力图数据
     */
    List<Map<String, Object>> selectDropoffHeatmap(@Param("date") String date, @Param("dataSource") String dataSource, @Param("limit") int limit);

    /**
     * 查询可用日期
     */
    List<String> selectAvailableDates();

    /**
     * 查询上车热点区域
     */
    List<Map<String, Object>> selectPickupHotspotZones(@Param("date") String date, @Param("topN") int topN);

    /**
     * 查询下车热点区域
     */
    List<Map<String, Object>> selectDropoffHotspotZones(@Param("date") String date, @Param("topN") int topN);
}
package com.taxi.analytics.modules.map.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.map.service.MapHeatmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Map", description = "地图热力图API")
@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {

    private final MapHeatmapService heatmapService;

    @Operation(summary = "获取上车点热力图数据", description = "返回GeoJSON格式的上车点热力图数据")
    @GetMapping("/pickup-heatmap")
    public Result<List<Map<String, Object>>> getPickupHeatmap(
            @Parameter(description = "日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "区域筛选：all/pickup/dropoff")
            @RequestParam(defaultValue = "pickup") String zoneType,
            @Parameter(description = "数据源：green/yellow/all")
            @RequestParam(defaultValue = "all") String dataSource,
            @Parameter(description = "返回点数限制")
            @RequestParam(defaultValue = "1000") int limit) {
        return Result.success(heatmapService.getPickupHeatmap(date, zoneType, dataSource, limit));
    }

    @Operation(summary = "获取下车点热力图数据", description = "返回GeoJSON格式的下车点热力图数据")
    @GetMapping("/dropoff-heatmap")
    public Result<List<Map<String, Object>>> getDropoffHeatmap(
            @Parameter(description = "日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "区域筛选：all/pickup/dropoff")
            @RequestParam(defaultValue = "dropoff") String zoneType,
            @Parameter(description = "数据源：green/yellow/all")
            @RequestParam(defaultValue = "all") String dataSource,
            @Parameter(description = "返回点数限制")
            @RequestParam(defaultValue = "1000") int limit) {
        return Result.success(heatmapService.getDropoffHeatmap(date, zoneType, dataSource, limit));
    }

    @Operation(summary = "获取上下车点对比热力图", description = "同时返回上车点和下车点热力图数据用于对比")
    @GetMapping("/combined-heatmap")
    public Result<Map<String, Object>> getCombinedHeatmap(
            @Parameter(description = "日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "数据源：green/yellow/all")
            @RequestParam(defaultValue = "all") String dataSource,
            @Parameter(description = "返回点数限制")
            @RequestParam(defaultValue = "500") int limit) {
        return Result.success(heatmapService.getCombinedHeatmap(date, dataSource, limit));
    }

    @Operation(summary = "获取热力图可用日期范围")
    @GetMapping("/available-dates")
    public Result<List<LocalDate>> getAvailableDates() {
        return Result.success(heatmapService.getAvailableDates());
    }

    @Operation(summary = "获取热点区域统计", description = "返回热点区域的聚合统计")
    @GetMapping("/hotspot-zones")
    public Result<Map<String, Object>> getHotspotZones(
            @Parameter(description = "日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "热点类型：pickup/dropoff")
            @RequestParam(defaultValue = "pickup") String hotspotType,
            @Parameter(description = "返回数量限制")
            @RequestParam(defaultValue = "20") int topN) {
        return Result.success(heatmapService.getHotspotZones(date, hotspotType, topN));
    }
}
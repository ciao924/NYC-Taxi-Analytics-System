package com.taxi.analytics.modules.realtime.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.realtime.service.RealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Realtime", description = "实时看板API")
@RestController
@RequestMapping("/realtime")
@RequiredArgsConstructor
public class RealtimeController {

    private final RealtimeService realtimeService;

    @Operation(summary = "获取最新5分钟KPI")
    @GetMapping("/kpi")
    public Result<Map<String, Object>> getLatestKpi() {
        return Result.success(realtimeService.getLatestKpi());
    }

    @Operation(summary = "获取最新5分钟热点区域")
    @GetMapping("/hotspot")
    public Result<List<Map<String, Object>>> getHotspot(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(realtimeService.getHotspot(limit));
    }

    @Operation(summary = "获取实时费用构成")
    @GetMapping("/fee")
    public Result<List<Map<String, Object>>> getFeeComposition() {
        return Result.success(realtimeService.getFeeComposition());
    }

    @Operation(summary = "获取最近24小时趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend() {
        return Result.success(realtimeService.getTrend());
    }
}

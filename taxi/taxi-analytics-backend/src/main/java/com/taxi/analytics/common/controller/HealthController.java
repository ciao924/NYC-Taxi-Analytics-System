package com.taxi.analytics.common.controller;

import com.taxi.analytics.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
public class HealthController {

    @GetMapping
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("message", "Taxi Analytics Backend is running");
        data.put("version", "1.0.0");
        return Result.success(data);
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        return Result.success(data);
    }
}

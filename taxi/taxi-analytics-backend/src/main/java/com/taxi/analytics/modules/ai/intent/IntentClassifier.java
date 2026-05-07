package com.taxi.analytics.modules.ai.intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifier.class);

    private final Map<IntentType, List<String>> queryPatterns = new HashMap<>();

    public IntentClassifier() {
        initializePatterns();
    }

    private void initializePatterns() {
        queryPatterns.put(IntentType.DATA_QUERY, Arrays.asList(
            "查询", "统计", "展示", "显示", "多少", "几个", "排行", "排名",
            "最近", "今天", "昨天", "本周", "本月", "趋势", "对比",
            "select", "show", "count", "sum", "avg", "max", "min"
        ));

        queryPatterns.put(IntentType.ETL_GENERATION, Arrays.asList(
            "生成", "创建", "编写", "ETL", "Spark", "清洗", "转换",
            "同步", "加工", "处理", "insert", "into", " overwrite"
        ));

        queryPatterns.put(IntentType.DIAGNOSTICS, Arrays.asList(
            "诊断", "排查", "问题", "错误", "失败", "异常", "告警",
            "为什么", "原因", "出错", "卡住", "慢", "diagnose", "issue"
        ));

        queryPatterns.put(IntentType.OPTIMIZATION, Arrays.asList(
            "优化", "调优", "加快", "提升", "改善", "性能",
            "parallelism", "优化", "tune", "optimize"
        ));
    }

    public Intent classify(QueryContext context) {
        String query = context.getQuery().toLowerCase();

        double maxScore = 0.0;
        IntentType detectedIntent = IntentType.GENERAL;

        for (Map.Entry<IntentType, List<String>> entry : queryPatterns.entrySet()) {
            IntentType intentType = entry.getKey();
            List<String> patterns = entry.getValue();
            double score = 0.0;

            for (String pattern : patterns) {
                if (query.contains(pattern.toLowerCase())) {
                    score += 1.0;
                }
            }

            if (score > maxScore) {
                maxScore = score;
                detectedIntent = intentType;
            }
        }

        Map<String, String> entities = extractEntities(context.getQuery());

        double confidence = maxScore > 0 ? Math.min(maxScore / 3.0, 1.0) : 0.5;

        log.info("Intent classified: {}, confidence: {}, entities: {}", detectedIntent.name(), confidence, entities);

        return new Intent(detectedIntent, entities, confidence);
    }

    private Map<String, String> extractEntities(String query) {
        Map<String, String> entities = new HashMap<>();

        List<String> timePatterns = Arrays.asList("今天", "昨天", "本周", "本月", "最近", "上周", "下周");
        for (String pattern : timePatterns) {
            if (query.contains(pattern)) {
                entities.put("time", pattern);
            }
        }

        List<String> metricPatterns = Arrays.asList("订单", "交易", "金额", "用户", "数量", "订单量");
        for (String pattern : metricPatterns) {
            if (query.contains(pattern)) {
                entities.put("metric", pattern);
            }
        }

        List<String> dimensionPatterns = Arrays.asList("供应商", "机场", "支付方式", "城市", "区域", "司机");
        for (String pattern : dimensionPatterns) {
            if (query.contains(pattern)) {
                entities.put("dimension", pattern);
            }
        }

        return entities;
    }
}

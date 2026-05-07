package com.taxi.analytics.modules.ai.service;

import com.taxi.analytics.modules.ai.client.ChatMessage;
import com.taxi.analytics.modules.ai.service.dto.*;

import java.util.List;

public interface AiService {

    AiQueryResponse processQuery(AiQueryRequest request);

    EtlGenResponse generateEtlSql(EtlGenRequest request);

    FieldMappingResponse mapFields(FieldMapRequest request);

    SkewDiagnoseResponse diagnoseSkew(SkewDiagnoseRequest request);

    TaskDiagnoseResponse diagnoseTask(TaskDiagnoseRequest request);

    FlinkBackpressureResponse diagnoseFlinkBackpressure(FlinkBackpressureRequest request);

    ParallelismRecommendResponse recommendParallelism(ParallelismRecommendRequest request);

    SuggestRulesResponse suggestQualityRules(SuggestRulesRequest request);

    AlertNormalizeResponse normalizeAlert(AlertNormalizeRequest request);

    List<ChatMessage> getSessionHistory(String sessionId);
}

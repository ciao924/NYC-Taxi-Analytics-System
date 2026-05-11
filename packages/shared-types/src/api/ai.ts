import { ChartConfig } from './common';

export interface AiQueryRequest {
  query: string;
  sessionId?: string;
  database?: string;
}

export interface AiQueryResponse {
  sessionId: string;
  sql: string;
  explanation: string;
  data: Record<string, unknown>[];
  chartConfig?: ChartConfig;
}

export interface EtlGenRequest {
  sourceTable: string;
  targetTable: string;
  description: string;
}

export interface EtlGenResponse {
  sparkSql: string;
  explanation: string;
  fieldMappings: FieldMapping[];
}

export interface FieldMapping {
  sourceField: string;
  targetField: string;
  transform: string;
}

export interface FieldMapRequest {
  sourceTable: string;
  targetTable: string;
}

export interface FieldMappingResponse {
  mappings: FieldMapping[];
  confidence: number;
}

export interface SkewDiagnoseRequest {
  jobId: string;
  executionPlan: string;
}

export interface SkewDiagnoseResponse {
  hasSkew: boolean;
  skewedStages: string[];
  skewFactor: number;
  recommendations: string[];
}

export interface TaskDiagnoseRequest {
  taskId: string;
  taskLog: string;
}

export interface TaskDiagnoseResponse {
  rootCause: string;
  category: string;
  suggestions: string[];
  confidence: number;
}

export interface FlinkBackpressureRequest {
  jobId: string;
  operatorMetrics: string[];
}

export interface FlinkBackpressureResponse {
  hasBackpressure: boolean;
  bottleneckOperator: string;
  backpressureRatio: number;
  recommendations: string[];
}

export interface ParallelismRecommendRequest {
  jobId: string;
  currentThroughput: number;
  currentParallelism: number;
}

export interface ParallelismRecommendResponse {
  recommendedParallelism: number;
  expectedImprovement: number;
  operatorRecommendations: OperatorRecommendation[];
}

export interface OperatorRecommendation {
  operatorName: string;
  recommendedParallelism: number;
  reason: string;
}

export interface SuggestRulesRequest {
  tableName: string;
  columnSamples: string[];
}

export interface SuggestRulesResponse {
  suggestedRules: QualityRule[];
  confidence: number;
}

export interface QualityRule {
  ruleType: RuleType;
  field: string;
  threshold: number;
  description: string;
}

export type RuleType = 'NON_NULL' | 'UNIQUE' | 'RANGE' | 'REGEX' | 'ENUM' | 'REFERENTIAL_INTEGRITY';

export interface AlertNormalizeRequest {
  alertContent: string;
  alertType: string;
}

export interface AlertNormalizeResponse {
  normalizedAlert: string;
  severity: AlertSeverity;
  suggestedActions: string[];
}

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'UNKNOWN';

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp?: number;
}

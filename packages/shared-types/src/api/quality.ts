import { DateRangeParams } from './common';

export interface QualitySummary {
  overallScore: number;
  completenessScore: number;
  accuracyScore: number;
  uniquenessScore: number;
  timelinessScore: number;
}

export interface TableHealthStatus {
  tableName: string;
  status: HealthStatus;
  score: number;
  recordCount: number;
  lastUpdated: string;
}

export type HealthStatus = 'HEALTHY' | 'WARNING' | 'CRITICAL' | 'UNKNOWN';

export interface CompletenessDetail {
  tableName: string;
  columnName: string;
  completenessRate: number;
  expectedRate: number;
  status: HealthStatus;
}

export interface NullRateDetail {
  tableName: string;
  columnName: string;
  nullRate: number;
  threshold: number;
  status: HealthStatus;
}

export interface UniquenessDetail {
  tableName: string;
  columnName: string;
  duplicateCount: number;
  totalCount: number;
  uniquenessRate: number;
  status: HealthStatus;
}

export interface ConsistencyDetail {
  tableName: string;
  checkType: string;
  isConsistent: boolean;
  message: string;
}

export interface RangeDetail {
  tableName: string;
  columnName: string;
  minValue: number;
  maxValue: number;
  expectedMin: number;
  expectedMax: number;
  outOfRangeCount: number;
  status: HealthStatus;
}

export interface FreshnessDetail {
  tableName: string;
  latestRecordTime: string;
  expectedUpdateInterval: number;
  delayMinutes: number;
  status: HealthStatus;
}

export interface Alert {
  id: string;
  tableName: string;
  columnName: string;
  alertType: AlertType;
  severity: AlertSeverity;
  message: string;
  timestamp: string;
  resolved: boolean;
}

export type AlertType = 'COMPLETENESS' | 'NULL_RATE' | 'UNIQUENESS' | 'CONSISTENCY' | 'RANGE' | 'FRESHNESS';
export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface QualityHistory {
  date: string;
  overallScore: number;
  completenessScore: number;
  accuracyScore: number;
  uniquenessScore: number;
  timelinessScore: number;
}

export interface ThresholdConfig {
  id: string;
  tableName: string;
  columnName: string;
  ruleType: string;
  threshold: number;
  severity: AlertSeverity;
  enabled: boolean;
}

export interface QualityReport {
  date: string;
  summary: QualitySummary;
  tableHealth: TableHealthStatus[];
  alerts: Alert[];
  recommendations: string[];
}

export interface QualityQueryRequest extends DateRangeParams {
  tableName?: string;
}

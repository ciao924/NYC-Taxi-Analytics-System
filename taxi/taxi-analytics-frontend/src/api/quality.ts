import request from './request'

export interface QualityReport {
  reportDate: string
  totalRecords: number
  validRecords: number
  invalidRecords: number
  overallScore: number
  alertLevel: string
  ruleResults: RuleResult[]
}

export interface RuleResult {
  ruleName: string
  passed: boolean
  failedCount: number
  message: string
}

export interface QualityScore {
  scoreDate: string
  recordCheckScore: number
  ruleCheckScore: number
  nullCheckScore: number
  overallScore: number
  passed: boolean
  alertLevel: string
}

export interface AnomalyAlert {
  id: number
  alertType: string
  windowStart: string
  windowEnd: string
  currentValue: number
  expectedValue: number
  changeRate: number
  severity: string
  message: string
  createdAt: string
}

export interface ErrorRecord {
  id: number
  errorType: string
  recordData: string
  errorMessage: string
  createdAt: string
}

export interface RealtimeStatus {
  totalCount: number
  nullVendorIdCount: number
  nullFareCount: number
  negativeFareCount: number
  lastUpdateTime: string
}

export interface QualityRule {
  id: number
  ruleName: string
  ruleType: string
  threshold: number
  enabled: boolean
  description: string
}

export const qualityApi = {
  getQualityReports: (params?: { startDate?: string; endDate?: string }) => {
    return request.get('/quality/summary', { date: params?.startDate })
  },

  getDailyQualityScore: (params?: { startDate?: string; endDate?: string }) => {
    return request.get('/quality/history', params)
  },

  getErrorRecords: (params?: { pageNum?: number; pageSize?: number; errorType?: string }) => {
    return request.get('/quality/reports', params)
  },

  getRealtimeStatus: () => {
    return request.get('/quality/summary')
  },

  getAnomalyAlerts: (params?: { startDate?: string; endDate?: string; severity?: string }) => {
    return request.get('/quality/alerts', params)
  },

  getQualityRules: () => {
    return request.get('/quality/thresholds')
  },

  updateQualityRule: (id: number, data: Partial<QualityRule>) => {
    return request.put(`/quality/thresholds/${id}`, data)
  },

  getNullCheckResults: (date: string) => {
    return request.get('/quality/null-rate', { date })
  },

  getRecordCheckResults: (date: string) => {
    return request.get('/quality/uniqueness', { date })
  }
}
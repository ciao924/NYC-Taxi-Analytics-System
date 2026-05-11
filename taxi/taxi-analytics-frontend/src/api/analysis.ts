import request from './request'

export interface AirportStatistics {
  airport_code: string
  airport_name: string
  trip_count: number
  total_amount: number
  avg_amount: number
  avg_distance: number
}

export interface VendorComparison {
  vendor_id: string
  vendor_name: string
  trip_count: number
  total_amount: number
  avg_trip_distance: number
  market_share: number
}

export interface PaymentDistribution {
  payment_type: string
  payment_desc: string
  payment_code: string
  trip_count: number
  percentage: number
  total_amount: number
}

export interface DistanceDistribution {
  distance_range: string
  trip_count: number
  percentage: number
  avg_distance?: number
}

export interface DurationDistribution {
  duration_range: string
  trip_count: number
  percentage: number
  avg_duration?: number
}

export interface PassengerDistribution {
  passenger_range: string
  avg_passenger_count: number
  trip_count: number
  percentage: number
}

export interface TipDistribution {
  tip_range: string
  trip_count: number
  percentage: number
  avg_tip: number
  avg_tip_rate: number
}

export interface MultiDimensionAnalysisDTO {
  dimension1: string
  dimension1Name: string
  dimension2: string
  dimension2Name: string
  tripCount: number
  totalAmount: number
  avgAmount: number
  avgDistance: number
  percentage: number
  statDate?: string
}

export interface RootCauseAnalysis {
  dimension: string
  value: string
  contribution: number
  impactDescription: string
}

export interface AnomalyDetectionDTO {
  metricName: string
  metricDisplayName: string
  anomalyDate: string
  actualValue: number
  expectedValue: number
  deviationPercent: number
  anomalyLevel: string
  anomalyType: string
  description: string
  potentialCauses: string[]
  rootCauses: RootCauseAnalysis[]
}

export interface PredictionDTO {
  date: string
  metricName: string
  predictedValue: number
  lowerBound: number
  upperBound: number
  confidence: number
  trend: number
  trendDirection: string
}

export interface BusinessInsightDTO {
  insightId: string
  title: string
  category: string
  level: string
  description: string
  recommendation: string
  supportingData: string[]
  discoveryDate: string
  impactScore: number
}

export interface TrendAnalysisDTO {
  date: string
  tripCount: number
  totalAmount: number
  avgAmount: number
  avgDistance: number
  passengerCount: number
  growthRate: number
  movingAverage: number
}

export const analysisApi = {
  getAirportStatistics: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<AirportStatistics[]>('/analysis/airport', params)
  },

  getAirportDetailedStatistics: (params?: { startDate?: string; endDate?: string; airportCode?: string }) => {
    return request.get('/analysis/airport/detailed', params)
  },

  getVendorComparison: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<VendorComparison[]>('/analysis/vendor', params)
  },

  getVendorTrend: (params?: { startDate?: string; endDate?: string; vendorId?: string }) => {
    return request.get('/analysis/vendor/trend', params)
  },

  getPaymentDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<PaymentDistribution[]>('/analysis/payment', params)
  },

  getPaymentTrend: (params?: { startDate?: string; endDate?: string }) => {
    return request.get('/analysis/payment/trend', params)
  },

  getDistanceDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<DistanceDistribution[]>('/analysis/distance/distribution', params)
  },

  getDurationDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<DurationDistribution[]>('/analysis/duration/distribution', params)
  },

  getPassengerDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<PassengerDistribution[]>('/analysis/passenger/distribution', params)
  },

  getTipDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<TipDistribution[]>('/analysis/tip/distribution', params)
  },

  getMultiDimensionAnalysis: (params?: { startDate?: string; endDate?: string; dimension1?: string; dimension2?: string }) => {
    return request.get<MultiDimensionAnalysisDTO[]>('/analysis/multi-dimension', params)
  },

  detectAnomalies: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<AnomalyDetectionDTO[]>('/analysis/anomaly-detection', params)
  },

  getPredictions: (params?: { startDate?: string; endDate?: string; days?: number }) => {
    return request.get<PredictionDTO[]>('/analysis/prediction', params)
  },

  generateBusinessInsights: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<BusinessInsightDTO[]>('/analysis/insights', params)
  },

  getTrendAnalysis: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<TrendAnalysisDTO[]>('/analysis/trend', params)
  },

  getCrossTabAnalysis: (params?: { startDate?: string; endDate?: string; rowDimension?: string; colDimension?: string }) => {
    return request.get<Record<string, any>[]>('/analysis/cross-tab', params)
  },

  getKpiSummary: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<{ trip_count: number; total_revenue: number; avg_fare: number; avg_distance: number }>('/analysis/kpi-summary', params)
  }
}

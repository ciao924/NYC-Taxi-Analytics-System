import request from './request'

export interface AirportStatistics {
  airport_code: string
  airport_name: string
  trip_count: number
  total_amount: number
  avg_amount: number
  avg_distance: number
  percentage?: number
}

export interface VendorComparison {
  vendor_id: string
  vendor_name: string
  trip_count: number
  total_amount: number
  avg_trip_distance: number
  market_share: number
  revenue_ratio?: number
}

export interface PaymentDistribution {
  payment_type: string
  payment_desc: string
  payment_code: string
  trip_count: number
  percentage: number
  total_amount: number
  avg_amount?: number
  total_tip?: number
  avg_tip?: number
  is_cashless?: number
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

export interface HourlyDistribution {
  hour: number
  trip_count: number
  avg_fare: number
  avg_tip: number
  total_revenue: number
  peak_hour?: boolean
}

export interface WeekdayAnalysis {
  day_of_week: number
  day_name: string
  is_weekend: boolean
  trip_count: number
  total_revenue: number
  avg_fare: number
  avg_distance?: number
}

export interface FeeComposition {
  fee_code: string
  fee_name: string
  total_amount: number
  percentage: number
}

export interface BoroughRevenue {
  borough: string
  trip_count: number
  total_revenue: number
  avg_fare: number
  total_tip: number
  avg_tip: number
  revenue_ratio: number
}

export interface BoroughFlow {
  pickup_borough: string
  dropoff_borough: string
  trip_count: number
  total_revenue?: number
  avg_distance?: number
}

export interface PickupHotspot {
  zone_name: string
  borough: string
  service_zone: string
  trip_count: number
  total_revenue: number
}

export interface DropoffHotspot {
  zone_name: string
  borough: string
  service_zone: string
  trip_count: number
}

export interface TaxiTypeFee {
  taxi_type: string
  trip_count: number
  total_revenue: number
  avg_fare: number
  total_tip: number
  avg_tip: number
  tip_rate: number
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

export interface CrossTabDTO {
  rowDimension: string
  rowValue: string
  colDimension: string
  colValue: string
  tripCount: number
  totalAmount: number
  percentage: number
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

export interface KpiSummary {
  trip_count: number
  total_revenue: number
  avg_fare: number
  avg_distance: number
  avg_duration?: number
  total_tip?: number
  avg_tip?: number
  airport_trips?: number
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

  getHourlyDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<HourlyDistribution[]>('/analysis/hourly/distribution', params)
  },

  getWeekdayAnalysis: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<WeekdayAnalysis[]>('/analysis/weekday/analysis', params)
  },

  getFeeComposition: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<FeeComposition[]>('/analysis/fee/composition', params)
  },

  getBoroughRevenue: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<BoroughRevenue[]>('/analysis/borough/revenue', params)
  },

  getBoroughFlow: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<BoroughFlow[]>('/analysis/borough/flow', params)
  },

  getPickupHotspots: (params?: { startDate?: string; endDate?: string; limit?: number }) => {
    return request.get<PickupHotspot[]>('/analysis/hotspots/pickup', params)
  },

  getDropoffHotspots: (params?: { startDate?: string; endDate?: string; limit?: number }) => {
    return request.get<DropoffHotspot[]>('/analysis/hotspots/dropoff', params)
  },

  getTaxiTypeFee: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<TaxiTypeFee[]>('/analysis/fee/taxi-type', params)
  },

  getMultiDimensionAnalysis: (params?: { startDate?: string; endDate?: string; dimension1?: string; dimension2?: string }) => {
    return request.get<MultiDimensionAnalysisDTO[]>('/analysis/multi-dimension', params)
  },

  getCrossTabAnalysis: (params?: { startDate?: string; endDate?: string; rowDimension?: string; colDimension?: string }) => {
    return request.get<CrossTabDTO[]>('/analysis/cross-tab', params)
  },

  getVendorPaymentCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/vendor-payment', params)
  },

  getAirportTimeCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/airport-time', params)
  },

  getBoroughPaymentCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/borough-payment', params)
  },

  getVendorTaxiTypeCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/vendor-taxitype', params)
  },

  getAirportBoroughCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/airport-borough', params)
  },

  getTimePaymentCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/time-payment', params)
  },

  getDistancePaymentCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/distance-payment', params)
  },

  getWeekdayTimeCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/weekday-time', params)
  },

  getTaxiTypeFeeCross: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<any[]>('/analysis/cross/taxitype-fee', params)
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

  getKpiSummary: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<KpiSummary>('/analysis/kpi-summary', params)
  }
}
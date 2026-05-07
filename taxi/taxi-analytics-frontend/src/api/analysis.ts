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
}

export interface DurationDistribution {
  duration_range: string
  trip_count: number
  percentage: number
}

export const analysisApi = {
  /**
   * 获取机场统计
   */
  getAirportStatistics: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<AirportStatistics[]>('/analysis/airport', params)
  },

  /**
   * 获取机场详细统计
   */
  getAirportDetailedStatistics: (params?: { startDate?: string; endDate?: string; airportCode?: string }) => {
    return request.get('/analysis/airport/detailed', params)
  },

  /**
   * 获取供应商对比
   */
  getVendorComparison: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<VendorComparison[]>('/analysis/vendor', params)
  },

  /**
   * 获取供应商趋势
   */
  getVendorTrend: (params?: { startDate?: string; endDate?: string; vendorId?: string }) => {
    return request.get('/analysis/vendor/trend', params)
  },

  /**
   * 获取支付方式分布
   */
  getPaymentDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<PaymentDistribution[]>('/analysis/payment', params)
  },

  /**
   * 获取支付方式趋势
   */
  getPaymentTrend: (params?: { startDate?: string; endDate?: string }) => {
    return request.get('/analysis/payment/trend', params)
  },

  /**
   * 获取行程距离分布
   */
  getDistanceDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<DistanceDistribution[]>('/analysis/distance/distribution', params)
  },

  /**
   * 获取行程时长分布
   */
  getDurationDistribution: (params?: { startDate?: string; endDate?: string }) => {
    return request.get<DurationDistribution[]>('/analysis/duration/distribution', params)
  }
}
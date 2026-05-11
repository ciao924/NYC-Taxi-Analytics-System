import request from './request'

export interface KpiSummary {
  tripCount: number
  totalRevenue: number
  avgFare: number
  avgDistance: number
}

export interface TrendData {
  statDate: string
  totalTrips: number
  totalRevenue: number
  avgFare: number
}

export interface PaymentDistribution {
  paymentType: string
  paymentTypeName: string
  tripCount: number
  percentage: number
}

export interface HourlyDistribution {
  hour: number
  tripCount: number
  avgFare: number
  totalRevenue: number
}

export interface VendorData {
  vendorName: string
  tripCount: number
  totalRevenue: number
  avgFare: number
  avgDistance: number
}

export interface DateRangeParams {
  startDate?: string
  endDate?: string
}

export const dashboardApi = {
  getKpiSummary: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get<KpiSummary>('/dashboard/kpi/summary', params)
  },

  getKpiTrend: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get<TrendData[]>('/dashboard/kpi/trend', params)
  },

  getHourlyDistribution: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get<HourlyDistribution[]>('/dashboard/hourly/distribution', params)
  },

  getWeekdayAnalysis: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/dashboard/weekday/analysis', params)
  },

  getPaymentAnalysis: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get<PaymentDistribution[]>('/dashboard/payment/analysis', params)
  },

  getVendorAnalysis: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get<VendorData[]>('/dashboard/vendor/analysis', params)
  },

  getFeeComposition: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/dashboard/fee/composition', params)
  },

  getFeePercentage: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/dashboard/fee/percentage', params)
  },

  getZoneHotspots: (startDate?: string, endDate?: string, limit?: number) => {
    const params: Record<string, string | number> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    if (limit) params.limit = limit
    return request.get('/dashboard/zone/hotspots', params)
  },

  getBoroughFlow: (startDate?: string, endDate?: string) => {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/dashboard/borough/flow', params)
  },

  getAvailableDateRange: () => {
    return request.get('/dashboard/date/range')
  },

  refreshCache: () => {
    return request.post('/dashboard/refresh')
  }
}

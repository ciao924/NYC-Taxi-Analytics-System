import request from './request'

export interface RealtimeKpi {
  orderCount: number
  totalFare: number
  avgFare: number
  windowEnd: string
  orderGrowth: number
  fareGrowth: number
  avgFareGrowth: number
}

export interface HotspotData {
  zone: string
  trip_count: number
  rank: number
}

export interface FeeComposition {
  payment_type: string
  trip_count: number
  total_fare: number
  avg_fare: number
  tip_rate: number
}

export interface RealtimeTrendData {
  hour: string
  trip_count: number
  total_fare: number
}

export const realtimeApi = {
  getKpi: () => {
    return request.get<RealtimeKpi>('/realtime/kpi')
  },

  getHotspot: (limit: number = 10) => {
    return request.get<HotspotData[]>('/realtime/hotspot', { limit })
  },

  getFeeComposition: () => {
    return request.get<FeeComposition[]>('/realtime/fee')
  },

  getTrend: () => {
    return request.get<RealtimeTrendData[]>('/realtime/trend')
  }
}
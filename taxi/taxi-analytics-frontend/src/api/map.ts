import request from './request'

export interface HeatmapPoint {
  lat: number
  lng: number
  count: number
  location_id?: number
  zone_name?: string
}

export interface HotspotZone {
  location_id: number
  zone_name: string
  trip_count: number
  total_amount: number
  avg_amount: number
}

export interface CombinedHeatmap {
  pickup: HeatmapPoint[]
  dropoff: HeatmapPoint[]
  date: string
  totalPickupCount: number
  totalDropoffCount: number
}

export const mapApi = {
  /**
   * 获取上车点热力图数据
   */
  getPickupHeatmap: (params?: { date?: string; zoneType?: string; dataSource?: string; limit?: number }) => {
    return request.get<HeatmapPoint[]>('/map/pickup-heatmap', params)
  },

  /**
   * 获取下车点热力图数据
   */
  getDropoffHeatmap: (params?: { date?: string; zoneType?: string; dataSource?: string; limit?: number }) => {
    return request.get<HeatmapPoint[]>('/map/dropoff-heatmap', params)
  },

  /**
   * 获取上下车点对比热力图
   */
  getCombinedHeatmap: (params?: { date?: string; dataSource?: string; limit?: number }) => {
    return request.get<CombinedHeatmap>('/map/combined-heatmap', params)
  },

  /**
   * 获取热力图可用日期范围
   */
  getAvailableDates: () => {
    return request.get<string[]>('/map/available-dates')
  },

  /**
   * 获取热点区域统计
   */
  getHotspotZones: (params?: { date?: string; hotspotType?: string; topN?: number }) => {
    return request.get<{ zones: HotspotZone[]; type: string }>('/map/hotspot-zones', params)
  }
}
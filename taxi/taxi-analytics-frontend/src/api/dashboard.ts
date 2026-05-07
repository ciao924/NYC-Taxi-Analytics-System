import request from './request'

// 仪表盘 API
export const dashboardApi = {
  /**
   * 获取KPI汇总数据
   */
  getKpiSummary: (params?: any) => {
    return request.get('/dashboard/kpi/summary', params)
  },

  /**
   * 获取趋势图数据
   */
  getTrendData: (params?: any) => {
    return request.get('/dashboard/kpi/trend', params)
  },

  /**
   * 获取小时分布数据
   */
  getHourlyDistribution: (params?: any) => {
    return request.get('/dashboard/hourly/distribution', params)
  },

  /**
   * 获取星期分析数据
   */
  getWeekdayAnalysis: (params?: any) => {
    return request.get('/dashboard/weekday/analysis', params)
  },

  /**
   * 获取支付方式分析
   */
  getPaymentAnalysis: (params?: any) => {
    return request.get('/dashboard/payment/analysis', params)
  },

  /**
   * 获取费用构成
   */
  getFeeComposition: (params?: any) => {
    return request.get('/dashboard/fee/composition', params)
  },

  /**
   * 获取费用占比
   */
  getFeePercentage: (params?: any) => {
    return request.get('/dashboard/fee/percentage', params)
  },

  /**
   * 获取上下客热点
   */
  getZoneHotspots: (params?: any) => {
    return request.get('/dashboard/zone/hotspots', params)
  },

  /**
   * 获取行政区流量
   */
  getBoroughFlow: (params?: any) => {
    return request.get('/dashboard/borough/flow', params)
  },

  /**
   * 获取可用日期范围
   */
  getAvailableDateRange: () => {
    return request.get('/dashboard/date/range')
  },

  /**
   * 手动刷新缓存
   */
  refreshCache: () => {
    return request.post('/dashboard/refresh')
  }
}
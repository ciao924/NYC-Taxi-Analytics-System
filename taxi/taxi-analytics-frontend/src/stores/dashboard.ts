import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardApi } from '@/api/dashboard'

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

export const useDashboardStore = defineStore('dashboard', () => {
  const kpiSummary = ref<KpiSummary | null>(null)
  const kpiTrend = ref<TrendData[]>([])
  const paymentAnalysis = ref<PaymentDistribution[]>([])
  const hourlyDistribution = ref<HourlyDistribution[]>([])
  const vendorAnalysis = ref<VendorData[]>([])

  const fetchKpiSummary = async (startDate?: string, endDate?: string) => {
    try {
      const response = await dashboardApi.getKpiSummary(startDate, endDate)
      kpiSummary.value = response
    } catch (error) {
      console.error('Failed to fetch KPI summary:', error)
    }
  }

  const fetchKpiTrend = async (startDate?: string, endDate?: string) => {
    try {
      const response = await dashboardApi.getKpiTrend(startDate, endDate)
      kpiTrend.value = response
    } catch (error) {
      console.error('Failed to fetch KPI trend:', error)
    }
  }

  const fetchPaymentAnalysis = async (startDate?: string, endDate?: string) => {
    try {
      const response = await dashboardApi.getPaymentAnalysis(startDate, endDate)
      paymentAnalysis.value = response
    } catch (error) {
      console.error('Failed to fetch payment analysis:', error)
    }
  }

  const fetchHourlyDistribution = async (startDate?: string, endDate?: string) => {
    try {
      const response = await dashboardApi.getHourlyDistribution(startDate, endDate)
      hourlyDistribution.value = response
    } catch (error) {
      console.error('Failed to fetch hourly distribution:', error)
    }
  }

  const fetchVendorAnalysis = async (startDate?: string, endDate?: string) => {
    try {
      const response = await dashboardApi.getVendorAnalysis(startDate, endDate)
      vendorAnalysis.value = response
    } catch (error) {
      console.error('Failed to fetch vendor analysis:', error)
    }
  }

  const clearData = () => {
    kpiSummary.value = null
    kpiTrend.value = []
    paymentAnalysis.value = []
    hourlyDistribution.value = []
    vendorAnalysis.value = []
  }

  return {
    kpiSummary,
    kpiTrend,
    paymentAnalysis,
    hourlyDistribution,
    vendorAnalysis,
    fetchKpiSummary,
    fetchKpiTrend,
    fetchPaymentAnalysis,
    fetchHourlyDistribution,
    fetchVendorAnalysis,
    clearData
  }
})

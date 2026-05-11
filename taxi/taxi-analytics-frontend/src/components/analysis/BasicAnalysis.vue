<template>
  <div class="basic-analysis-container">
    <div class="basic-analysis-header">
      <h2 class="section-title">基础业务分析</h2>
    </div>

    <div class="basic-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.name"
        class="basic-tab-item"
        :class="{ active: activeTab === tab.name }"
        @click="switchTab(tab.name)"
      >
        {{ tab.label }}
      </div>
    </div>

    <div class="tab-content">
      <!-- 机场运营分析 -->
      <div v-show="activeTab === 'airport'" class="analysis-panel">
        <div class="panel-header">
          <h3 class="panel-title">机场运营分析</h3>
          <p class="panel-desc">三大机场（JFK、LGA、EWR）的订单量、收入及运营效率分析</p>
        </div>
        
        <div class="stats-grid">
          <div
            v-for="airport in airportStats"
            :key="airport.airport_code"
            class="stat-card"
          >
            <div class="stat-header">
              <span class="stat-title">{{ airport.airport_code }}</span>
              <span class="stat-subtitle">{{ airport.airport_name }}</span>
            </div>
            <div class="stat-body">
              <div class="stat-row">
                <span class="stat-label">订单数</span>
                <span class="stat-value">{{ formatNumber(airport.trip_count) }}</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">总收入</span>
                <span class="stat-value">${{ formatNumber(airport.total_amount, 2) }}</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">平均金额</span>
                <span class="stat-value">${{ formatNumber(airport.avg_amount, 2) }}</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">平均里程</span>
                <span class="stat-value">{{ formatNumber(airport.avg_distance, 2) }} mi</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-wrapper">
          <v-chart :option="airportChartOption" autoresize class="chart" />
        </div>
      </div>

      <!-- 供应商绩效分析 -->
      <div v-show="activeTab === 'vendor'" class="analysis-panel">
        <div class="panel-header">
          <h3 class="panel-title">供应商绩效分析</h3>
          <p class="panel-desc">各供应商的市场份额、订单量及收入贡献分析</p>
        </div>

        <div class="stats-grid">
          <div
            v-for="vendor in vendorStats"
            :key="vendor.vendor_id"
            class="stat-card"
          >
            <div class="stat-header">
                  <span class="stat-title">{{ formatVendorName(vendor.vendor_name) }}</span>
                  <span class="stat-subtitle">ID: {{ vendor.vendor_id }}</span>
                </div>
            <div class="stat-body">
              <div class="stat-row">
                <span class="stat-label">订单数</span>
                <span class="stat-value">{{ formatNumber(vendor.trip_count) }}</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">市场份额</span>
                <span class="stat-value">{{ formatNumber(vendor.market_share, 1) }}%</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">总收入</span>
                <span class="stat-value">${{ formatNumber(vendor.total_amount, 2) }}</span>
              </div>
              <div class="stat-row">
                <span class="stat-label">平均里程</span>
                <span class="stat-value">{{ formatNumber(vendor.avg_trip_distance, 2) }} mi</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-wrapper">
          <v-chart :option="vendorChartOption" autoresize class="chart" />
        </div>
      </div>

      <!-- 支付方式分析 -->
      <div v-show="activeTab === 'payment'" class="analysis-panel">
        <div class="panel-header">
          <h3 class="panel-title">支付方式分析</h3>
          <p class="panel-desc">各支付渠道的订单分布及金额占比分析</p>
        </div>

        <div class="summary-cards">
          <div class="summary-card">
            <div class="summary-label">总订单数</div>
            <div class="summary-value">{{ formatNumber(totalPaymentTrips) }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">总金额</div>
            <div class="summary-value">${{ formatNumber(totalPaymentAmount, 2) }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">主流支付占比</div>
            <div class="summary-value">{{ formatNumber(dominantPaymentPercentage, 1) }}%</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">支付方式种类</div>
            <div class="summary-value">{{ paymentStats.length }} 种</div>
          </div>
        </div>

        <div class="payment-chart-row">
          <div class="payment-chart-card">
            <h4 class="chart-title">支付方式订单分布</h4>
            <div v-if="paymentStats && paymentStats.length > 0" class="chart-container">
              <v-chart :option="paymentPieChartOption" autoresize class="small-chart" />
            </div>
            <div v-else class="empty-chart-frame">
              <div class="empty-icon">📊</div>
              <div class="empty-text">暂无数据</div>
              <div class="empty-hint">请选择日期范围查询数据</div>
            </div>
          </div>
          <div class="payment-chart-card">
            <h4 class="chart-title">支付方式金额占比</h4>
            <div v-if="paymentStats && paymentStats.length > 0" class="chart-container">
              <v-chart :option="paymentBarChartOption" autoresize class="small-chart" />
            </div>
            <div v-else class="empty-chart-frame">
              <div class="empty-icon">📈</div>
              <div class="empty-text">暂无数据</div>
              <div class="empty-hint">请选择日期范围查询数据</div>
            </div>
          </div>
        </div>

        <div class="payment-detail-table">
          <table>
            <thead>
              <tr>
                <th>支付方式</th>
                <th>订单数</th>
                <th>占比</th>
                <th>总金额</th>
                <th>平均金额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="payment in paymentStats" :key="payment.payment_type">
                <td>{{ payment.payment_desc }}</td>
                <td>{{ formatNumber(payment.trip_count) }}</td>
                <td>{{ formatNumber(calculatePaymentPercentage(payment.trip_count), 1) }}%</td>
                <td>${{ formatNumber(payment.total_amount, 2) }}</td>
                <td>${{ formatNumber(payment.trip_count > 0 ? payment.total_amount / payment.trip_count : 0, 2) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 行程特征分析 -->
      <div v-show="activeTab === 'trip'" class="analysis-panel">
        <div class="panel-header">
          <h3 class="panel-title">行程特征分析</h3>
          <p class="panel-desc">乘客数量分布、小费比率分布及关键指标分析</p>
        </div>

        <div class="summary-cards">
          <div class="summary-card">
            <div class="summary-label">平均行程距离</div>
            <div class="summary-value">{{ formatNumber(avgDistance, 1) }} 英里</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">平均行程时长</div>
            <div class="summary-value">{{ formatDuration(avgDuration) }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">平均乘客数</div>
            <div class="summary-value">{{ formatNumber(avgPassengerCount, 1) }} 人</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">平均小费比率</div>
            <div class="summary-value">{{ formatNumber(avgTipRate, 1) }}%</div>
          </div>
        </div>

        <div class="trip-chart-row">
          <div class="trip-chart-card">
            <h4 class="chart-title">乘客数量分布</h4>
            <div v-if="passengerStats && passengerStats.length > 0" class="chart-container">
              <v-chart :option="passengerChartOption" autoresize class="small-chart" />
              <div class="chart-summary">
                <span class="summary-text">最常见: {{ mostCommonPassengerRange }} ({{ mostCommonPassengerPercentage }}%)</span>
              </div>
            </div>
            <div v-else class="empty-chart-frame">
              <div class="empty-icon">👥</div>
              <div class="empty-text">暂无数据</div>
              <div class="empty-hint">请选择日期范围查询数据</div>
            </div>
          </div>
          <div class="trip-chart-card">
            <h4 class="chart-title">小费比率分布</h4>
            <div v-if="tipStats && tipStats.length > 0" class="chart-container">
              <v-chart :option="tipChartOption" autoresize class="small-chart" />
              <div class="chart-summary">
                <span class="summary-text">最高占比: {{ highestTipRange }} ({{ highestTipPercentage }}%)</span>
              </div>
            </div>
            <div v-else class="empty-chart-frame">
              <div class="empty-icon">💰</div>
              <div class="empty-text">暂无数据</div>
              <div class="empty-hint">请选择日期范围查询数据</div>
            </div>
          </div>
        </div>

        <div v-if="tripInsights && tripInsights.length > 0" class="insight-cards">
          <h4 class="insights-section-title">数据洞察与运营建议</h4>
          <div class="insights-grid">
            <div 
              v-for="(insight, index) in tripInsights" 
              :key="index"
              class="insight-card"
              :class="insight.level"
            >
              <div class="insight-header">
                <span class="insight-category">{{ insight.category }}</span>
                <span class="insight-level-badge">{{ getInsightLevelLabel(insight.level) }}</span>
              </div>
              <h5 class="insight-title">{{ insight.title }}</h5>
              <p class="insight-description">{{ insight.description }}</p>
              <div class="insight-recommendation">
                <span class="recommendation-label">建议:</span>
                <span class="recommendation-text">{{ insight.recommendation }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent
} from 'echarts/components'
import * as echarts from 'echarts/core'
import {
  AirportStatistics,
  VendorComparison,
  PaymentDistribution,
  DistanceDistribution,
  DurationDistribution,
  PassengerDistribution,
  TipDistribution
} from '@/api/analysis'

use([
  CanvasRenderer,
  BarChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  VisualMapComponent
])

interface TripInsight {
  category: string
  level: string
  title: string
  description: string
  recommendation: string
}

const props = defineProps<{
  airportStats: AirportStatistics[]
  vendorStats: VendorComparison[]
  paymentStats: PaymentDistribution[]
  distanceStats: DistanceDistribution[]
  durationStats: DurationDistribution[]
  passengerStats: PassengerDistribution[]
  tipStats: TipDistribution[]
  tripInsights?: TripInsight[]
}>()

const activeTab = ref('airport')

const tabs = [
  { name: 'airport', label: '机场运营' },
  { name: 'vendor', label: '供应商绩效' },
  { name: 'payment', label: '支付方式' },
  { name: 'trip', label: '行程特征' }
]

const totalPaymentTrips = computed(() => {
  return props.paymentStats.reduce((sum, p) => sum + p.trip_count, 0)
})

const totalPaymentAmount = computed(() => {
  return props.paymentStats.reduce((sum, p) => sum + p.total_amount, 0)
})

const dominantPaymentPercentage = computed(() => {
  if (totalPaymentTrips.value === 0) return 0
  const maxTrips = Math.max(...props.paymentStats.map(p => p.trip_count))
  return (maxTrips / totalPaymentTrips.value) * 100
})

const calculatePaymentPercentage = (tripCount: number): number => {
  if (totalPaymentTrips.value === 0) return 0
  return (tripCount / totalPaymentTrips.value) * 100
}

const avgDistance = computed(() => {
  if (!props.distanceStats || props.distanceStats.length === 0) return 0
  const statsWithAvgDistance = props.distanceStats.filter(d => d.avg_distance && d.avg_distance > 0)
  if (statsWithAvgDistance.length > 0) {
    let totalWeightedDistance = 0
    let totalTrips = 0
    statsWithAvgDistance.forEach(d => {
      totalWeightedDistance += (d.avg_distance || 0) * (d.trip_count || 0)
      totalTrips += d.trip_count || 0
    })
    return totalTrips > 0 ? Math.round((totalWeightedDistance / totalTrips) * 10) / 10 : 0
  }
  let totalDistance = 0
  let totalTrips = 0
  props.distanceStats.forEach(d => {
    const range = d.distance_range
    let avgDist = 0
    if (range === '0-5英里' || range === '0-5') avgDist = 2.5
    else if (range === '5-10英里' || range === '5-10') avgDist = 7.5
    else if (range === '10-20英里' || range === '10-20') avgDist = 15
    else if (range === '20-50英里' || range === '20-50') avgDist = 35
    else if (range === '50-120英里' || range === '50+') avgDist = 85
    else if (range === '0-2') avgDist = 1
    else if (range === '2-5') avgDist = 3.5
    else if (range === '10-15') avgDist = 12.5
    else if (range === '15-20') avgDist = 17.5
    else if (range === '20+') avgDist = 25
    totalDistance += avgDist * (d.trip_count || 0)
    totalTrips += d.trip_count || 0
  })
  return totalTrips > 0 ? Math.round((totalDistance / totalTrips) * 10) / 10 : 0
})

const avgDuration = computed(() => {
  if (!props.durationStats || props.durationStats.length === 0) return 0
  const statsWithAvgDuration = props.durationStats.filter(d => d.avg_duration && d.avg_duration > 0)
  if (statsWithAvgDuration.length > 0) {
    let totalWeightedDuration = 0
    let totalTrips = 0
    statsWithAvgDuration.forEach(d => {
      totalWeightedDuration += (d.avg_duration || 0) * (d.trip_count || 0)
      totalTrips += d.trip_count || 0
    })
    return totalTrips > 0 ? Math.round((totalWeightedDuration / totalTrips) * 10) / 10 : 0
  }
  let totalDuration = 0
  let totalTrips = 0
  props.durationStats.forEach(d => {
    const range = d.duration_range
    let avgDur = 0
    if (range === '0-10分钟' || range === '0-10') avgDur = 5
    else if (range === '10-20分钟' || range === '10-20') avgDur = 15
    else if (range === '20-30分钟' || range === '20-30') avgDur = 25
    else if (range === '30-60分钟' || range === '30-45') avgDur = 45
    else if (range === '60分钟以上' || range === '45-60' || range === '60+') avgDur = 90
    totalDuration += avgDur * (d.trip_count || 0)
    totalTrips += d.trip_count || 0
  })
  return totalTrips > 0 ? Math.round((totalDuration / totalTrips) * 10) / 10 : 0
})

const avgPassengerCount = computed(() => {
  if (!props.passengerStats || props.passengerStats.length === 0) return 0
  let totalWeightedPassengers = 0
  let totalTrips = 0
  props.passengerStats.forEach(p => {
    totalWeightedPassengers += (p.avg_passenger_count || 0) * (p.trip_count || 0)
    totalTrips += p.trip_count || 0
  })
  return totalTrips > 0 ? Math.round((totalWeightedPassengers / totalTrips) * 10) / 10 : 0
})

const avgTipRate = computed(() => {
  if (!props.tipStats || props.tipStats.length === 0) return 0
  let totalWeightedTipRate = 0
  let totalTrips = 0
  props.tipStats.forEach(t => {
    totalWeightedTipRate += (t.avg_tip_rate || 0) * (t.trip_count || 0)
    totalTrips += t.trip_count || 0
  })
  return totalTrips > 0 ? Math.round((totalWeightedTipRate / totalTrips) * 10) / 10 : 0
})

const mostCommonPassengerRange = computed(() => {
  if (!props.passengerStats || props.passengerStats.length === 0) return '-'
  let maxCount = 0
  let maxRange = '-'
  props.passengerStats.forEach(p => {
    if ((p.trip_count || 0) > maxCount) {
      maxCount = p.trip_count || 0
      maxRange = p.passenger_range || '-'
    }
  })
  return maxRange
})

const mostCommonPassengerPercentage = computed(() => {
  if (!props.passengerStats || props.passengerStats.length === 0) return '0'
  const total = props.passengerStats.reduce((sum, p) => sum + (p.trip_count || 0), 0)
  if (total === 0) return '0'
  const max = Math.max(...props.passengerStats.map(p => p.trip_count || 0))
  return ((max / total) * 100).toFixed(1)
})

const highestTipRange = computed(() => {
  if (!props.tipStats || props.tipStats.length === 0) return '-'
  let maxPercentage = 0
  let maxRange = '-'
  props.tipStats.forEach(t => {
    if ((t.percentage || 0) > maxPercentage) {
      maxPercentage = t.percentage || 0
      maxRange = t.tip_range || '-'
    }
  })
  return maxRange
})

const highestTipPercentage = computed(() => {
  if (!props.tipStats || props.tipStats.length === 0) return '0'
  const max = Math.max(...props.tipStats.map(t => t.percentage || 0))
  return max.toFixed(1)
})

const formatDuration = (minutes: number): string => {
  if (minutes < 60) {
    return `${Math.round(minutes)} 分钟`
  } else {
    const hours = Math.floor(minutes / 60)
    const mins = Math.round(minutes % 60)
    return `${hours}小时${mins}分钟`
  }
}

const getInsightLevelLabel = (level: string): string => {
  const levelMap: Record<string, string> = {
    high: '高优先级',
    medium: '中优先级',
    low: '低优先级',
    critical: '严重'
  }
  return levelMap[level] || level
}

const formatNumber = (num: number | undefined, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const formatVendorName = (name: string): string => {
  if (!name) return ''
  return name.replace(/^[\d\s]+/, '').trim()
}

const switchTab = (tabName: string) => {
  activeTab.value = tabName
}

const airportChartOption = computed(() => {
  const stats = props.airportStats || []
  
  return {
    tooltip: { 
      trigger: 'axis', 
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const airport = stats.find(a => a.airport_code === params[0].axisValue)
        if (!airport) return ''
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">${airport.airport_code} - ${airport.airport_name}</div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(airport.trip_count)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>总收入:</span><span style="font-weight: 600; color: #3b82f6;">$${formatNumber(airport.total_amount, 2)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>平均金额:</span><span style="font-weight: 600;">$${formatNumber(airport.avg_amount, 2)}</span>
                </div>`
      }
    },
    legend: { 
      data: ['订单数', '收入'], 
      top: 0,
      textStyle: { color: '#6b7280', fontSize: 10 },
      itemWidth: 12,
      itemHeight: 8,
      itemGap: 16
    },
    grid: { left: '12%', right: '8%', bottom: '18%', top: '18%', containLabel: true },
    xAxis: {
      type: 'category',
      data: stats.map(a => a.airport_code),
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 10,
        fontWeight: 600,
        margin: 12
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: [
      { 
        type: 'value', 
        name: '订单数',
        nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -25] },
        axisLabel: { 
          color: '#6b7280', 
          fontSize: 9,
          formatter: (value: number) => {
            if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
            else if (value >= 1000) return (value / 1000).toFixed(1) + 'K'
            return value.toString()
          }
        },
        splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      { 
        type: 'value', 
        name: '收入',
        nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -25] },
        position: 'right',
        axisLabel: { 
          color: '#6b7280', 
          fontSize: 9,
          formatter: (value: number) => {
            if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M'
            else if (value >= 1000) return (value / 1000).toFixed(1) + 'K'
            return value.toString()
          }
        },
        splitLine: { show: false },
        axisLine: { show: false },
        axisTick: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: stats.map(a => a.trip_count),
        barWidth: '35%',
        itemStyle: { 
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#3b82f6' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(59, 130, 246, 0.5)'
          }
        }
      },
      {
        name: '收入',
        type: 'bar',
        yAxisIndex: 1,
        data: stats.map(a => a.total_amount),
        barWidth: '35%',
        itemStyle: { 
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#86efac' },
            { offset: 1, color: '#22c55e' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(34, 197, 94, 0.5)'
          }
        }
      }
    ],
    animationDuration: 1200,
    animationEasing: 'elasticOut'
  }
})

const vendorChartOption = computed(() => {
  const stats = props.vendorStats || []
  const colors = ['#3b82f6', '#22c55e']
  
  return {
    tooltip: { 
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const vendor = stats.find(v => formatVendorName(v.vendor_name) === params.name)
        if (!vendor) return ''
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">${params.name}</div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(vendor.trip_count)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>总收入:</span><span style="font-weight: 600; color: #3b82f6;">$${formatNumber(vendor.total_amount, 2)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>市场份额:</span><span style="font-weight: 600;">${vendor.market_share.toFixed(1)}%</span>
                </div>`
      }
    },
    legend: { 
      orient: 'horizontal', 
      bottom: '0%',
      left: 'center',
      textStyle: { color: '#6b7280', fontSize: 10 },
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 20,
      padding: [0, 0, 10, 0]
    },
    series: [{
      name: '市场份额',
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '42%'],
      label: { 
        show: true, 
        formatter: '{b}\n{d}%',
        fontSize: 10,
        color: '#374151',
        alignTo: 'edge',
        edgeDistance: 5
      },
      labelLine: {
        length: 8,
        length2: 6,
        smooth: true
      },
      data: stats.map((v, index) => ({ 
        name: formatVendorName(v.vendor_name), 
        value: v.market_share || 0,
        itemStyle: {
          color: colors[index % colors.length],
          borderRadius: 4
        }
      })),
      emphasis: {
        scale: true,
        scaleSize: 6,
        itemStyle: {
          shadowBlur: 12,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      animationType: 'scale',
      animationEasing: 'elasticOut',
      animationDelay: (idx: number) => idx * 80
    }]
  }
})

const paymentPieChartOption = computed(() => {
  const stats = props.paymentStats || []
  const colors = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6']
  
  return {
    tooltip: { 
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const payment = stats.find(p => p.payment_desc === params.name)
        if (!payment) return ''
        const percentage = calculatePaymentPercentage(payment.trip_count || 0)
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">${params.name}</div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(payment.trip_count)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>占比:</span><span style="font-weight: 600; color: ${params.color};">${percentage.toFixed(1)}%</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>总金额:</span><span style="font-weight: 600;">$${formatNumber(payment.total_amount, 2)}</span>
                </div>`
      }
    },
    legend: { 
      orient: 'horizontal', 
      bottom: '0%',
      left: 'center',
      textStyle: { color: '#6b7280', fontSize: 10 },
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 14,
      padding: [0, 0, 8, 0]
    },
    series: [{
      name: '支付方式',
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '42%'],
      label: { 
        show: true, 
        formatter: '{b}\n{d}%',
        fontSize: 10,
        color: '#374151',
        alignTo: 'edge',
        edgeDistance: 5
      },
      labelLine: {
        length: 8,
        length2: 6,
        smooth: true
      },
      data: stats.map((p, index) => ({ 
        name: p.payment_desc, 
        value: p.trip_count || 0,
        itemStyle: {
          color: colors[index % colors.length],
          borderRadius: 4
        }
      })),
      emphasis: {
        scale: true,
        scaleSize: 6,
        itemStyle: {
          shadowBlur: 12,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      animationType: 'scale',
      animationEasing: 'elasticOut',
      animationDelay: (idx: number) => idx * 60
    }]
  }
})

const paymentBarChartOption = computed(() => {
  const stats = props.paymentStats || []
  
  return {
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const payment = stats.find(p => p.payment_desc === params[0].axisValue)
        if (!payment) return ''
        const avgAmount = (payment.trip_count || 0) > 0 ? payment.total_amount / payment.trip_count : 0
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">${payment.payment_desc}</div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(payment.trip_count)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>总金额:</span><span style="font-weight: 600; color: #3b82f6;">$${formatNumber(payment.total_amount, 2)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>平均金额:</span><span style="font-weight: 600;">$${formatNumber(avgAmount, 2)}</span>
                </div>`
      }
    },
    grid: { left: '14%', right: '8%', bottom: '20%', top: '12%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: stats.map(p => p.payment_desc),
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 10,
        fontWeight: 500,
        rotate: stats.length > 3 ? 25 : 0,
        interval: 0,
        margin: 12,
        align: 'center'
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: { 
      type: 'value', 
      name: '金额($)',
      nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -30] },
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 9,
        formatter: (value: number) => {
          if (value >= 1000000) {
            return (value / 1000000).toFixed(1) + 'M'
          } else if (value >= 1000) {
            return (value / 1000).toFixed(1) + 'K'
          }
          return value.toString()
        }
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [{
      name: '总金额',
      type: 'bar',
      data: stats.map(p => p.total_amount || 0),
      barWidth: '45%',
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#1d4ed8' }
        ]),
        borderRadius: [4, 4, 0, 0],
        borderColor: '#188df0',
        borderWidth: 1
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(24, 141, 240, 0.4)',
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#b3d9ff' },
            { offset: 1, color: '#188df0' }
          ])
        }
      },
      animationDuration: 1000,
      animationEasing: 'elasticOut'
    }]
  }
})

const passengerChartOption = computed(() => {
  const stats = props.passengerStats || []
  
  return {
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const axisValue = params[0]?.axisValue || ''
        const data = stats.find(p => p.passenger_range === axisValue)
        if (!data) return ''
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">乘客数量: ${data.passenger_range}</div>
                <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(data.trip_count)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                  <span>占比:</span><span style="font-weight: 600; color: #3b82f6;">${data.percentage}%</span>
                </div>`
      }
    },
    grid: { left: '15%', right: '8%', bottom: '12%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: stats.map(p => p.passenger_range),
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 10,
        fontWeight: 500,
        margin: 12
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: { 
      type: 'value', 
      name: '订单数',
      nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -40] },
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 9,
        formatter: (value: number) => {
          if (value >= 1000000) {
            return (value / 1000000).toFixed(2) + 'M'
          } else if (value >= 1000) {
            return (value / 1000).toFixed(1) + 'K'
          }
          return value.toString()
        }
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false },
      boundaryGap: [0, 0.1]
    },
    series: [{
      name: '订单数',
      type: 'bar',
      data: stats.map(p => p.trip_count || 0),
      barWidth: '55%',
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#93c5fd' },
          { offset: 0.5, color: '#3b82f6' },
          { offset: 1, color: '#2563eb' }
        ]),
        borderRadius: [4, 4, 0, 0],
        borderColor: '#3b82f6',
        borderWidth: 1
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 12,
          shadowColor: 'rgba(59, 130, 246, 0.5)',
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#bfdbfe' },
            { offset: 1, color: '#3b82f6' }
          ])
        }
      },
      animationDuration: 1200,
      animationEasing: 'elasticOut'
    }]
  }
})

const tipChartOption = computed(() => {
  const stats = props.tipStats || []
  
  return {
    tooltip: { 
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const axisValue = params[0]?.axisValue || ''
        const data = stats.find(t => t.tip_range === axisValue)
        if (!data) return ''
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">小费比率: ${data.tip_range}</div>
                <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(data.trip_count)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                  <span>平均小费:</span><span style="font-weight: 600; color: #22c55e;">$${formatNumber(data.avg_tip, 2)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                  <span>占比:</span><span style="font-weight: 600; color: #f59e0b;">${data.percentage}%</span>
                </div>`
      }
    },
    grid: { left: '15%', right: '8%', bottom: '12%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: stats.map(t => t.tip_range),
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 9,
        fontWeight: 500,
        margin: 12,
        rotate: stats.length > 4 ? 30 : 0
      },
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisTick: { show: false }
    },
    yAxis: { 
      type: 'value', 
      name: '订单数',
      nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -40] },
      axisLabel: { 
        color: '#6b7280', 
        fontSize: 9,
        formatter: (value: number) => {
          if (value >= 1000000) {
            return (value / 1000000).toFixed(2) + 'M'
          } else if (value >= 1000) {
            return (value / 1000).toFixed(1) + 'K'
          }
          return value.toString()
        }
      },
      splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false },
      boundaryGap: [0, 0.1]
    },
    series: [{
      name: '订单数',
      type: 'bar',
      data: stats.map(t => t.trip_count || 0),
      barWidth: '55%',
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#fcd34d' },
          { offset: 0.5, color: '#f59e0b' },
          { offset: 1, color: '#d97706' }
        ]),
        borderRadius: [4, 4, 0, 0],
        borderColor: '#f59e0b',
        borderWidth: 1
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 12,
          shadowColor: 'rgba(245, 158, 11, 0.5)',
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#fde68a' },
            { offset: 1, color: '#f59e0b' }
          ])
        }
      },
      animationDuration: 1200,
      animationEasing: 'elasticOut'
    }]
  }
})

watch(() => props.airportStats, () => {}, { deep: true })
watch(() => props.vendorStats, () => {}, { deep: true })
watch(() => props.paymentStats, () => {}, { deep: true })
watch(() => props.distanceStats, () => {}, { deep: true })
watch(() => props.durationStats, () => {}, { deep: true })
watch(() => props.passengerStats, () => {}, { deep: true })
watch(() => props.tipStats, () => {}, { deep: true })

onMounted(() => {})

onUnmounted(() => {})
</script>

<style lang="scss" scoped>
.basic-analysis-container {
  width: 100%;
}

.basic-analysis-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.basic-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 8px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.basic-tab-item {
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f3f4f6;
  }

  &.active {
    background: #409eff;
    color: #ffffff;
  }
}

.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.analysis-panel {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.panel-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.panel-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
}

.stat-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.stat-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-right: 8px;
}

.stat-subtitle {
  font-size: 12px;
  color: #6b7280;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

.stat-value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.chart-wrapper {
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%);
  border: 1px solid #e0f2fe;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}

.summary-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #0369a1;
}

.trip-chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.trip-chart-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  min-height: 380px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.small-chart {
  height: 280px;
}

.chart-summary {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.summary-text {
  font-size: 13px;
  color: #6b7280;
}

.insight-cards {
  margin-top: 24px;
}

.insights-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 16px 0;
}

.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
}

.insight-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid #6b7280;
  
  &.high {
    border-left-color: #ef4444;
    background: linear-gradient(135deg, #fef2f2 0%, #f9fafb 100%);
  }
  
  &.medium {
    border-left-color: #f59e0b;
    background: linear-gradient(135deg, #fffbeb 0%, #f9fafb 100%);
  }
  
  &.low {
    border-left-color: #22c55e;
    background: linear-gradient(135deg, #f0fdf4 0%, #f9fafb 100%);
  }
  
  &.critical {
    border-left-color: #dc2626;
    background: linear-gradient(135deg, #fef2f2 0%, #f9fafb 100%);
  }
}

.insight-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.insight-category {
  font-size: 12px;
  font-weight: 500;
  color: #6b7280;
  background: #e5e7eb;
  padding: 4px 10px;
  border-radius: 12px;
}

.insight-level-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 10px;
  
  .high & {
    background: #fee2e2;
    color: #dc2626;
  }
  
  .medium & {
    background: #fef3c7;
    color: #d97706;
  }
  
  .low & {
    background: #dcfce7;
    color: #16a34a;
  }
  
  .critical & {
    background: #fee2e2;
    color: #991b1b;
  }
}

.insight-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.insight-description {
  font-size: 13px;
  color: #4b5563;
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.insight-recommendation {
  display: flex;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.recommendation-label {
  font-size: 12px;
  font-weight: 600;
  color: #3b82f6;
}

.recommendation-text {
  font-size: 12px;
  color: #374151;
}

.payment-chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.payment-chart-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
}

.payment-detail-table {
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px;
  overflow-x: auto;
}

.payment-detail-table table {
  width: 100%;
  border-collapse: collapse;
}

.payment-detail-table th,
.payment-detail-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.payment-detail-table th {
  background: #f3f4f6;
  font-weight: 600;
  color: #374151;
}

.payment-detail-table tr:hover {
  background: #f3f4f6;
}

.empty-chart-frame {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  border: 2px dashed #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.empty-hint {
  font-size: 13px;
  color: #9ca3af;
}

.chart-container {
  height: 200px;
}
</style>
<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="header-left">
        <h2 class="page-title">数据看板</h2>
        <p class="page-subtitle">实时监控出租车运营数据</p>
      </div>
      <div class="header-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateRangeChange"
        />
        <el-button
          type="primary"
          @click="refreshData"
          :loading="loading"
        >
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div
        v-for="(kpi, index) in kpiList"
        :key="index"
        class="kpi-card"
      >
        <div class="kpi-indicator" :style="{ backgroundColor: kpi.color }"></div>
        <div class="kpi-content">
          <p class="kpi-label">{{ kpi.label }}</p>
          <p class="kpi-value">{{ formatNumber(kpi.value) }}</p>
          <p class="kpi-unit">{{ kpi.unit }}</p>
        </div>
        <div class="kpi-trend" :class="kpi.growth >= 0 ? 'positive' : 'negative'">
          <span>{{ kpi.growth >= 0 ? '+' : '' }}{{ kpi.growth }}%</span>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">订单趋势</h3>
          <div class="chart-actions">
            <el-button-group>
              <el-button
                v-for="period in periods"
                :key="period.value"
                :type="activePeriod === period.value ? 'primary' : 'default'"
                size="small"
                @click="handlePeriodChange(period.value)"
              >
                {{ period.label }}
              </el-button>
            </el-button-group>
          </div>
        </div>
        <div class="chart-body">
          <v-chart :option="trendChartOption" autoresize />
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">支付方式分布</h3>
        </div>
        <div class="chart-body">
          <v-chart :option="paymentChartOption" autoresize />
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">时段分布</h3>
        </div>
        <div class="chart-body">
          <v-chart :option="hourlyChartOption" autoresize />
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <h3 class="chart-title">供应商绩效</h3>
        </div>
        <div class="chart-body">
          <el-table :data="vendorData" stripe border>
            <el-table-column prop="vendorName" label="供应商" />
            <el-table-column prop="tripCount" label="订单数" />
            <el-table-column prop="totalRevenue" label="总收入(USD)" />
            <el-table-column prop="avgFare" label="平均费用(USD)" />
            <el-table-column prop="rating" label="评分">
              <template #default="scope">
                <el-rate :value="scope.row.rating" disabled :max="5" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboard'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import * as echarts from 'echarts/core'

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const store = useDashboardStore()
const loading = ref(false)
const dateRange = ref(['', ''])
const activePeriod = ref('7d')

const periods = [
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' },
  { label: '90天', value: '90d' }
]

const calculateDateRange = (period: string) => {
  const endDate = new Date('2025-01-31')
  let startDate = new Date('2025-01-01')
  
  switch (period) {
    case '7d':
      startDate = new Date('2025-01-25')
      break
    case '30d':
      startDate = new Date('2025-01-01')
      break
    case '90d':
      startDate = new Date('2024-11-02')
      break
  }
  
  const formatDate = (date: Date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  
  return [formatDate(startDate), formatDate(endDate)]
}

const kpiList = computed(() => [
  {
    label: '总订单数',
    value: store.kpiSummary?.tripCount || 0,
    unit: '单',
    growth: 12.5,
    color: '#409eff'
  },
  {
    label: '总收入',
    value: store.kpiSummary?.totalRevenue || 0,
    unit: 'USD',
    growth: 8.3,
    color: '#67c23a'
  },
  {
    label: '平均费用',
    value: store.kpiSummary?.avgFare || 0,
    unit: 'USD',
    growth: -2.1,
    color: '#e6a23c'
  },
  {
    label: '平均距离',
    value: store.kpiSummary?.avgDistance || 0,
    unit: '英里',
    growth: 3.7,
    color: '#f56c6c'
  }
])

const vendorData = computed(() => store.vendorAnalysis || [])

const trendChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e5e7eb',
    borderWidth: 1,
    padding: [10, 14],
    textStyle: { color: '#374151', fontSize: 11 },
    axisPointer: { type: 'cross', crossStyle: { color: '#9ca3af' } },
    formatter: (params: any) => {
      let result = `<div style="font-weight: 600; margin-bottom: 8px; color: #1f2937; font-size: 11px;">${params[0].axisValue}</div>`
      params.forEach((param: any) => {
        const value = param.seriesName === '收入' ? '$' + formatNumber(param.value, 2) : formatNumber(param.value)
        result += `<div style="display: flex; justify-content: space-between; gap: 30px; font-size: 10px; margin-top: 4px;">
          <span><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${param.color}; margin-right: 6px;"></span>${param.seriesName}</span>
          <span style="font-weight: 600;">${value}</span>
        </div>`
      })
      return result
    }
  },
  legend: {
    data: ['订单数', '收入'],
    top: 0,
    textStyle: { color: '#6b7280', fontSize: 10 },
    itemWidth: 12,
    itemHeight: 8,
    itemGap: 20
  },
  grid: { left: '10%', right: '8%', bottom: '12%', top: '18%', containLabel: true },
  xAxis: {
    type: 'category',
    data: store.kpiTrend.map((item: { statDate: string }) => item.statDate),
    axisLabel: { color: '#6b7280', fontSize: 10, margin: 12 },
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisTick: { show: false }
  },
  yAxis: [
    {
      type: 'value',
      name: '订单数',
      nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -30] },
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
      name: '收入(USD)',
      nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -30] },
      position: 'right',
      axisLabel: {
        color: '#6b7280',
        fontSize: 9,
        formatter: (value: number) => {
          if (value >= 1000000) return '$' + (value / 1000000).toFixed(1) + 'M'
          else if (value >= 1000) return '$' + (value / 1000).toFixed(1) + 'K'
          return '$' + value.toString()
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
      type: 'line',
      smooth: true,
      data: store.kpiTrend.map((item: { totalTrips: number }) => item.totalTrips),
      lineStyle: {
        width: 3,
        color: '#3b82f6'
      },
      itemStyle: {
        color: '#3b82f6',
        borderWidth: 2,
        borderColor: '#fff'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
        ])
      },
      emphasis: {
        scale: true,
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(59, 130, 246, 0.5)'
        }
      },
      animationDuration: 1500,
      animationEasing: 'cubicInOut'
    },
    {
      name: '收入',
      type: 'bar',
      yAxisIndex: 1,
      data: store.kpiTrend.map((item: { totalRevenue: number }) => item.totalRevenue),
      barWidth: '50%',
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
      },
      animationDuration: 1200,
      animationEasing: 'elasticOut'
    }
  ]
}))

const paymentChartOption = computed(() => {
  const stats = store.paymentAnalysis || []
  const colors = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6']
  const data = stats.map((item: { paymentTypeName: string; tripCount: number }, index: number) => ({
    name: item.paymentTypeName,
    value: item.tripCount || 0,
    itemStyle: {
      color: colors[index % colors.length],
      borderRadius: 4
    }
  }))
  
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      padding: [10, 14],
      textStyle: { color: '#374151', fontSize: 11 },
      formatter: (params: any) => {
        const payment = stats.find((p: { paymentTypeName: string }) => p.paymentTypeName === params.name)
        if (!payment) return ''
        const total = stats.reduce((sum: number, p: { tripCount: number }) => sum + (p.tripCount || 0), 0)
        const percentage = total > 0 ? ((payment.tripCount || 0) / total * 100).toFixed(1) : '0'
        return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">${params.name}</div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>订单数:</span><span style="font-weight: 600;">${formatNumber(payment.tripCount)}</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>占比:</span><span style="font-weight: 600; color: ${params.color};">${percentage}%</span>
                </div>
                <div style="display: flex; justify-content: space-between; gap: 20px; font-size: 10px;">
                  <span>总金额:</span><span style="font-weight: 600;">$${formatNumber((payment as any).totalAmount, 2)}</span>
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
      data,
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

const hourlyChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e5e7eb',
    borderWidth: 1,
    padding: [10, 14],
    textStyle: { color: '#374151', fontSize: 11 },
    axisPointer: { type: 'shadow' },
    formatter: (params: any) => {
      const hour = params[0].axisValue
      const data = store.hourlyDistribution.find((item: { hour: number }) => `${item.hour}:00` === hour)
      if (!data) return ''
      return `<div style="font-weight: 600; margin-bottom: 6px; color: #1f2937; font-size: 11px;">时段: ${hour}</div>
              <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                <span>订单数:</span><span style="font-weight: 600;">${formatNumber(data.tripCount)}</span>
              </div>
              <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                <span>平均费用:</span><span style="font-weight: 600; color: #3b82f6;">$${formatNumber((data as any).avgFare, 2)}</span>
              </div>
              <div style="display: flex; justify-content: space-between; gap: 25px; font-size: 10px;">
                <span>总收入:</span><span style="font-weight: 600; color: #22c55e;">$${formatNumber((data as any).totalRevenue, 2)}</span>
              </div>`
    }
  },
  grid: { left: '10%', right: '8%', bottom: '12%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    data: store.hourlyDistribution.map((item: { hour: number }) => `${item.hour}:00`),
    axisLabel: { color: '#6b7280', fontSize: 9, margin: 12, rotate: 45 },
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisTick: { show: false }
  },
  yAxis: {
    type: 'value',
    name: '订单数',
    nameTextStyle: { color: '#6b7280', fontSize: 9, padding: [0, 0, 0, -35] },
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
  series: [{
    name: '订单数',
    type: 'bar',
    data: store.hourlyDistribution.map((item: { tripCount: number }) => item.tripCount || 0),
    barWidth: '60%',
    itemStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#a78bfa' },
        { offset: 0.5, color: '#8b5cf6' },
        { offset: 1, color: '#7c3aed' }
      ]),
      borderRadius: [4, 4, 0, 0],
      borderColor: '#8b5cf6',
      borderWidth: 1
    },
    emphasis: {
      itemStyle: {
        shadowBlur: 12,
        shadowColor: 'rgba(139, 92, 246, 0.5)',
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#c4b5fd' },
          { offset: 1, color: '#8b5cf6' }
        ])
      }
    },
    animationDuration: 1200,
    animationEasing: 'elasticOut'
  }]
}))

const formatNumber = (num: number | undefined, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const handleDateRangeChange = () => {
  activePeriod.value = ''
  fetchData()
}

const refreshData = () => {
  fetchData()
}

const handlePeriodChange = (period: string) => {
  activePeriod.value = period
  dateRange.value = calculateDateRange(period)
}

const fetchData = async () => {
  if (!dateRange.value[0] || !dateRange.value[1]) {
    dateRange.value = calculateDateRange(activePeriod.value)
  }
  
  loading.value = true
  try {
    await store.fetchKpiSummary(dateRange.value[0], dateRange.value[1])
    await store.fetchKpiTrend(dateRange.value[0], dateRange.value[1])
    await store.fetchPaymentAnalysis(dateRange.value[0], dateRange.value[1])
    await store.fetchHourlyDistribution(dateRange.value[0], dateRange.value[1])
    await store.fetchVendorAnalysis(dateRange.value[0], dateRange.value[1])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  dateRange.value = calculateDateRange(activePeriod.value)
  fetchData()
})

watch(activePeriod, (newPeriod) => {
  if (newPeriod) {
    dateRange.value = calculateDateRange(newPeriod)
    fetchData()
  }
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.kpi-indicator {
  width: 8px;
  height: 48px;
  border-radius: 4px;
}

.kpi-content {
  flex: 1;
}

.kpi-label {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 4px 0;
}

.kpi-value {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.kpi-unit {
  font-size: 12px;
  color: #9ca3af;
  margin: 0;
}

.kpi-trend {
  font-size: 14px;
  font-weight: 500;

  &.positive {
    color: #67c23a;
  }

  &.negative {
    color: #f56c6c;
  }
}

.chart-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.chart-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.chart-body {
  padding: 20px;
  height: 320px;
}
</style>

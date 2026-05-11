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
                @click="activePeriod = period.value"
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

use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const store = useDashboardStore()
const loading = ref(false)
const dateRange = ref(['2025-01-01', '2025-01-07'])
const activePeriod = ref('7d')

const periods = [
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' },
  { label: '90天', value: '90d' }
]

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

const vendorData = ref([
  { vendorName: 'VTS', tripCount: 12580, totalRevenue: 285670, avgFare: 22.71, rating: 4.5 },
  { vendorName: 'DDS', tripCount: 9845, totalRevenue: 221430, avgFare: 22.49, rating: 4.3 },
  { vendorName: 'Green', tripCount: 7632, totalRevenue: 171720, avgFare: 22.50, rating: 4.1 }
])

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['订单数', '收入'] },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: store.kpiTrend.map((item: { statDate: string }) => item.statDate)
  },
  yAxis: [
    { type: 'value', name: '订单数' },
    { type: 'value', name: '收入(USD)', axisLabel: { formatter: '{value}' } }
  ],
  series: [
    {
      name: '订单数',
      type: 'line',
      data: store.kpiTrend.map((item: { totalTrips: number }) => item.totalTrips)
    },
    {
      name: '收入',
      type: 'bar',
      yAxisIndex: 1,
      data: store.kpiTrend.map((item: { totalRevenue: number }) => item.totalRevenue)
    }
  ]
}))

const paymentChartOption = computed(() => {
  const data = store.paymentAnalysis.map((item: { paymentTypeName: string; percentage: number }) => ({
    name: item.paymentTypeName,
    value: item.percentage
  }))
  return {
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c}% ({d}%)' },
    legend: { orient: 'vertical', right: '5%', top: 'center' },
    series: [{ type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'], data }]
  }
})

const hourlyChartOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: store.hourlyDistribution.map((item: { hour: number }) => `${item.hour}:00`)
  },
  yAxis: { type: 'value', name: '订单数' },
  series: [{ type: 'bar', data: store.hourlyDistribution.map((item: { tripCount: number }) => item.tripCount) }]
}))

const formatNumber = (num: number | undefined) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

const handleDateRangeChange = () => {
  fetchData()
}

const refreshData = () => {
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    await store.fetchKpiSummary(dateRange.value[0], dateRange.value[1])
    await store.fetchKpiTrend(dateRange.value[0], dateRange.value[1])
    await store.fetchPaymentAnalysis(dateRange.value[0], dateRange.value[1])
    await store.fetchHourlyDistribution(dateRange.value[0], dateRange.value[1])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

watch(activePeriod, () => {
  fetchData()
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

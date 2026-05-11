<template>
  <div class="analysis-page">
    <el-card class="analysis-card">
      <template #header>
        <div class="card-header">
          <h2>数据分析</h2>
          <div class="header-controls">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              class="date-picker"
            />
            <el-button type="primary" @click="loadAllData" :loading="loading">
              查询数据
            </el-button>
          </div>
        </div>
      </template>

      <!-- 数据状态 -->
      <div class="data-status" v-if="showStatusBar">
        <el-tag :type="dataStatusType" size="small">
          {{ dataStatusText }}
        </el-tag>
        <span class="update-info">最后更新：{{ lastUpdateTime }}</span>
      </div>

      <!-- 加载状态 -->
      <el-skeleton :loading="loading" animated>
        <template #template>
          <el-tabs type="border-card">
            <el-tab-pane label="加载中">
              <el-skeleton-item variant="rect" style="height: 120px; margin-bottom: 16px" />
              <el-skeleton-item variant="rect" style="height: 300px" />
            </el-tab-pane>
          </el-tabs>
        </template>

        <!-- 错误状态 -->
        <div v-if="error" class="error-container">
          <el-alert
            :title="errorMessage"
            type="error"
            show-icon
            :closable="false"
            class="error-alert"
          />
          <el-button type="primary" @click="loadAllData" class="retry-button">
            重试
          </el-button>
        </div>

        <!-- 空状态 -->
        <div v-else-if="isEmptyData" class="empty-container">
          <el-empty description="暂无数据" :image-size="120">
            <el-button type="primary" @click="loadAllData">
              查询数据
            </el-button>
          </el-empty>
        </div>

        <!-- 数据展示区域 -->
        <el-tabs v-else v-model="activeTab" type="border-card" @tab-change="handleTabChange">
          <el-tab-pane label="机场统计" name="airport">
            <div class="tab-content">
              <el-row :gutter="20" class="kpi-row">
                <el-col :xs="12" :sm="8" v-for="airport in airportStats" :key="airport.airport_code">
                  <el-card shadow="hover" class="airport-card">
                    <div class="airport-info">
                      <div class="airport-code">{{ airport.airport_code }}</div>
                      <div class="airport-name">{{ airport.airport_name }}</div>
                    </div>
                    <div class="airport-stats">
                      <div class="stat-item">
                        <span class="stat-label">订单数</span>
                        <span class="stat-value">{{ formatNumber(airport.trip_count) }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">总收入</span>
                        <span class="stat-value">${{ formatNumber(airport.total_amount, 2) }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">平均金额</span>
                        <span class="stat-value">${{ formatNumber(airport.avg_amount, 2) }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">平均里程</span>
                        <span class="stat-value">{{ formatNumber(airport.avg_distance, 2) }} mi</span>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>

              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <span>机场订单对比</span>
                </template>
                <div ref="airportChartRef" class="chart-container"></div>
              </el-card>
            </div>
          </el-tab-pane>

          <el-tab-pane label="供应商对比" name="vendor">
            <div class="tab-content">
              <el-row :gutter="20" class="kpi-row">
                <el-col :xs="12" :sm="6" v-for="vendor in vendorStats" :key="vendor.vendor_id">
                  <el-card shadow="hover" class="vendor-card">
                    <div class="vendor-info">
                      <div class="vendor-id">{{ vendor.vendor_id }}</div>
                      <div class="vendor-name">{{ vendor.vendor_name }}</div>
                    </div>
                    <div class="vendor-stats">
                      <div class="stat-item">
                        <span class="stat-label">订单数</span>
                        <span class="stat-value">{{ formatNumber(vendor.trip_count) }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">市场份额</span>
                        <span class="stat-value">{{ formatNumber(vendor.market_share, 1) }}%</span>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>

              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <span>供应商市场份额</span>
                </template>
                <div ref="vendorChartRef" class="chart-container"></div>
              </el-card>
            </div>
          </el-tab-pane>

          <el-tab-pane label="支付方式" name="payment">
            <div class="tab-content">
              <el-row :gutter="20" class="kpi-row">
                <el-col :xs="12" :sm="6" v-for="payment in paymentStats" :key="payment.payment_type">
                  <el-card shadow="hover" class="payment-card">
                    <div class="payment-info">
                      <div class="payment-type">{{ payment.payment_desc }}</div>
                      <div class="payment-code">{{ payment.payment_code }}</div>
                    </div>
                    <div class="payment-stats">
                      <div class="stat-item">
                        <span class="stat-label">订单数</span>
                        <span class="stat-value">{{ formatNumber(payment.trip_count) }}</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">占比</span>
                        <span class="stat-value">{{ formatNumber(payment.percentage, 1) }}%</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-label">金额</span>
                        <span class="stat-value">${{ formatNumber(payment.total_amount, 2) }}</span>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>

              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <span>支付方式分布</span>
                </template>
                <div ref="paymentChartRef" class="chart-container"></div>
              </el-card>
            </div>
          </el-tab-pane>

          <el-tab-pane label="行程分析" name="trip">
            <div class="tab-content">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-card shadow="hover" class="chart-card">
                    <template #header>
                      <span>行程距离分布</span>
                    </template>
                    <div ref="distanceChartRef" class="chart-container"></div>
                  </el-card>
                </el-col>
                <el-col :span="12">
                  <el-card shadow="hover" class="chart-card">
                    <template #header>
                      <span>行程时长分布</span>
                    </template>
                    <div ref="durationChartRef" class="chart-container"></div>
                  </el-card>
                </el-col>
              </el-row>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { analysisApi } from '@/api/analysis'
import * as echarts from 'echarts'

const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const activeTab = ref('airport')
const dateRange = ref<[string, string]>(['2025-01-01', '2025-01-07'])
const lastUpdateTime = ref('2025-03-31 02:00:00')
const dataStatus = ref<'normal' | 'updating' | 'delay'>('normal')

const airportStats = ref<any[]>([])
const vendorStats = ref<any[]>([])
const paymentStats = ref<any[]>([])
const distanceStats = ref<any[]>([])
const durationStats = ref<any[]>([])

const airportChartRef = ref<HTMLElement | null>(null)
const vendorChartRef = ref<HTMLElement | null>(null)
const paymentChartRef = ref<HTMLElement | null>(null)
const distanceChartRef = ref<HTMLElement | null>(null)
const durationChartRef = ref<HTMLElement | null>(null)

let airportChart: echarts.ECharts | null = null
let vendorChart: echarts.ECharts | null = null
let paymentChart: echarts.ECharts | null = null
let distanceChart: echarts.ECharts | null = null
let durationChart: echarts.ECharts | null = null

const dataStatusType = computed(() => {
  const typeMap = {
    normal: 'success',
    updating: 'warning',
    delay: 'danger'
  }
  return typeMap[dataStatus.value]
})

const dataStatusText = computed(() => {
  const statusMap = {
    normal: '数据正常',
    updating: '数据更新中',
    delay: '数据延迟'
  }
  return statusMap[dataStatus.value]
})

const showStatusBar = computed(() => {
  return !loading.value && !error.value && !isEmptyData.value
})

const isEmptyData = computed(() => {
  return airportStats.value.length === 0 &&
         vendorStats.value.length === 0 &&
         paymentStats.value.length === 0 &&
         distanceStats.value.length === 0 &&
         durationStats.value.length === 0
})

const formatNumber = (num: number | undefined, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const loadAllData = async () => {
  if (!dateRange.value || dateRange.value.length !== 2 || !dateRange.value[0]) {
    ElMessage.warning('请选择日期范围')
    return
  }

  const [startDate, endDate] = dateRange.value
  
  if (startDate > endDate) {
    ElMessage.warning('开始日期不能大于结束日期')
    return
  }

  loading.value = true
  error.value = false

  try {
    const results = await Promise.all([
      analysisApi.getAirportStatistics({ startDate, endDate }),
      analysisApi.getVendorComparison({ startDate, endDate }),
      analysisApi.getPaymentDistribution({ startDate, endDate }),
      analysisApi.getDistanceDistribution({ startDate, endDate }),
      analysisApi.getDurationDistribution({ startDate, endDate })
    ])

    airportStats.value = results[0] || []
    vendorStats.value = results[1] || []
    paymentStats.value = results[2] || []
    distanceStats.value = results[3] || []
    durationStats.value = results[4] || []

    lastUpdateTime.value = new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })

    await nextTick()
    renderActiveTabChart()
  } catch (err: any) {
    error.value = true
    errorMessage.value = err.message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

const renderActiveTabChart = () => {
  switch (activeTab.value) {
    case 'airport':
      renderAirportChart()
      break
    case 'vendor':
      renderVendorChart()
      break
    case 'payment':
      renderPaymentChart()
      break
    case 'trip':
      renderDistanceChart()
      renderDurationChart()
      break
  }
}

const ensureChartContainer = (refEl: HTMLElement | null): boolean => {
  if (!refEl) return false
  const rect = refEl.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

const renderAirportChart = async () => {
  if (!ensureChartContainer(airportChartRef.value) || airportStats.value.length === 0) return

  await nextTick()

  if (airportChart) {
    airportChart.dispose()
    airportChart = null
  }

  try {
    airportChart = echarts.init(airportChartRef.value!)

    const option: echarts.EChartsOption = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['订单数', '收入'] },
      xAxis: {
        type: 'category',
        data: airportStats.value.map(a => a.airport_code)
      },
      yAxis: [
        { type: 'value', name: '订单数', position: 'left' },
        { type: 'value', name: '收入', position: 'right', axisLabel: { formatter: '${value}' } }
      ],
      series: [
        { name: '订单数', type: 'bar', data: airportStats.value.map(a => a.trip_count) },
        { name: '收入', type: 'bar', data: airportStats.value.map(a => a.total_amount) }
      ]
    }

    airportChart.setOption(option)
  } catch (err) {
    console.error('Failed to render airport chart:', err)
  }
}

const renderVendorChart = async () => {
  if (!ensureChartContainer(vendorChartRef.value) || vendorStats.value.length === 0) return

  await nextTick()

  if (vendorChart) {
    vendorChart.dispose()
    vendorChart = null
  }

  try {
    vendorChart = echarts.init(vendorChartRef.value!)

    const option: echarts.EChartsOption = {
      tooltip: { trigger: 'item', formatter: '{b}: {c}% ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [
        {
          name: '市场份额',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          label: { show: true, formatter: '{b}\n{c}%' },
          data: vendorStats.value.map(v => ({ name: v.vendor_name, value: v.market_share }))
        }
      ]
    }

    vendorChart.setOption(option)
  } catch (err) {
    console.error('Failed to render vendor chart:', err)
  }
}

const renderPaymentChart = async () => {
  if (!ensureChartContainer(paymentChartRef.value) || paymentStats.value.length === 0) return

  await nextTick()

  if (paymentChart) {
    paymentChart.dispose()
    paymentChart = null
  }

  try {
    paymentChart = echarts.init(paymentChartRef.value!)

    const validData = paymentStats.value.filter(p => p.trip_count > 0)
    
    const option: echarts.EChartsOption = {
      tooltip: { trigger: 'item', formatter: '{b}: 订单数 {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [
        {
          name: '支付方式',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          label: { show: true, formatter: '{b}\n{c}单' },
          data: validData.map(p => ({ name: p.payment_desc, value: p.trip_count }))
        }
      ]
    }

    paymentChart.setOption(option)
  } catch (err) {
    console.error('Failed to render payment chart:', err)
  }
}

const renderDistanceChart = async () => {
  if (!ensureChartContainer(distanceChartRef.value) || distanceStats.value.length === 0) return

  await nextTick()

  if (distanceChart) {
    distanceChart.dispose()
    distanceChart = null
  }

  try {
    distanceChart = echarts.init(distanceChartRef.value!)

    const option: echarts.EChartsOption = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: distanceStats.value.map(d => d.distance_range + ' mi') },
      yAxis: { type: 'value', name: '订单数' },
      series: [{ name: '订单数', type: 'bar', data: distanceStats.value.map(d => d.trip_count) }]
    }

    distanceChart.setOption(option)
  } catch (err) {
    console.error('Failed to render distance chart:', err)
  }
}

const renderDurationChart = async () => {
  if (!ensureChartContainer(durationChartRef.value) || durationStats.value.length === 0) return

  await nextTick()

  if (durationChart) {
    durationChart.dispose()
    durationChart = null
  }

  try {
    durationChart = echarts.init(durationChartRef.value!)

    const option: echarts.EChartsOption = {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: durationStats.value.map(d => d.duration_range + ' min') },
      yAxis: { type: 'value', name: '订单数' },
      series: [{ name: '订单数', type: 'bar', data: durationStats.value.map(d => d.trip_count) }]
    }

    durationChart.setOption(option)
  } catch (err) {
    console.error('Failed to render duration chart:', err)
  }
}

const handleTabChange = async () => {
  await nextTick()
  renderActiveTabChart()
}

const handleResize = () => {
  if (airportChart && ensureChartContainer(airportChartRef.value)) {
    airportChart.resize()
  }
  if (vendorChart && ensureChartContainer(vendorChartRef.value)) {
    vendorChart.resize()
  }
  if (paymentChart && ensureChartContainer(paymentChartRef.value)) {
    paymentChart.resize()
  }
  if (distanceChart && ensureChartContainer(distanceChartRef.value)) {
    distanceChart.resize()
  }
  if (durationChart && ensureChartContainer(durationChartRef.value)) {
    durationChart.resize()
  }
}

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  airportChart?.dispose()
  vendorChart?.dispose()
  paymentChart?.dispose()
  distanceChart?.dispose()
  durationChart?.dispose()
})

watch(activeTab, () => {
  handleTabChange()
})
</script>

<style scoped lang="scss">
.analysis-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.analysis-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 16px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }
  }
}

.data-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.update-info {
  font-size: 12px;
  color: #909399;
}

.error-container {
  text-align: center;
  padding: 40px 0;
}

.error-alert {
  max-width: 400px;
  margin: 0 auto 20px;
}

.retry-button {
  margin-top: 16px;
}

.empty-container {
  padding: 60px 0;
}

.tab-content {
  padding: 20px 0;
}

.kpi-row {
  margin-bottom: 20px;
}

.airport-card,
.vendor-card,
.payment-card {
  margin-bottom: 20px;

  .airport-info,
  .vendor-info,
  .payment-info {
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #eee;

    .airport-code,
    .vendor-id,
    .payment-type {
      font-size: 24px;
      font-weight: 700;
      color: #409eff;
    }

    .airport-name,
    .vendor-name,
    .payment-code {
      font-size: 14px;
      color: #666;
      margin-top: 4px;
    }
  }

  .airport-stats,
  .vendor-stats,
  .payment-stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;

    .stat-item {
      display: flex;
      flex-direction: column;

      .stat-label {
        font-size: 12px;
        color: #999;
      }

      .stat-value {
        font-size: 16px;
        font-weight: 600;
        color: #333;
      }
    }
  }
}

.chart-card {
  .chart-container {
    width: 100%;
    height: 300px;
    min-height: 300px;
  }
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-picker {
  width: 280px;
}
</style>
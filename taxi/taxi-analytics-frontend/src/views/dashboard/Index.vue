<template>
  <div class="dashboard-page">
    <el-card class="dashboard-card">
      <template #header>
        <div class="card-header">
          <h2>数据可视化模块</h2>
          <div class="header-controls">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="fetchDashboardData"
              class="date-picker"
            />
            <el-button type="primary" @click="fetchDashboardData" :loading="loading">
              查询数据
            </el-button>
          </div>
        </div>
      </template>

      <!-- 数据状态 -->
      <div class="data-status">
        <el-tag :type="dataStatusType" size="small">
          {{ dataStatusText }}
        </el-tag>
        <span class="update-info">最后更新：{{ lastUpdateTime }}</span>
      </div>

      <!-- 加载状态 -->
      <el-skeleton :loading="loading" animated>
        <template #template>
          <el-skeleton-item variant="p" style="width: 100%" />
          <el-skeleton-item variant="rect" style="height: 120px; margin-top: 16px" />
          <el-skeleton-item variant="rect" style="height: 300px; margin-top: 16px" />
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
          <el-button type="primary" @click="fetchDashboardData" class="retry-button">
            重试
          </el-button>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!kpiData" class="empty-container">
          <el-empty
            description="暂无数据"
            :image-size="120"
          >
            <el-button type="primary" @click="fetchDashboardData">
              查询数据
            </el-button>
          </el-empty>
        </div>

        <!-- 数据展示区域 -->
        <div v-else class="data-display">
          <!-- KPI 指标卡片区 -->
          <div class="kpi-section">
            <el-row :gutter="20">
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="kpi-card">
                  <div class="kpi-content">
                    <div class="kpi-title">总订单量</div>
                    <div class="kpi-value">{{ formatNumber(kpiData.trip_count, 0) }} 单</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="kpi-card">
                  <div class="kpi-content">
                    <div class="kpi-title">总收入</div>
                    <div class="kpi-value">¥{{ formatNumber(kpiData.total_revenue, 2) }}</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="kpi-card">
                  <div class="kpi-content">
                    <div class="kpi-title">平均客单价</div>
                    <div class="kpi-value">¥{{ formatNumber(kpiData.avg_fare, 2) }}</div>
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="kpi-card">
                  <div class="kpi-content">
                    <div class="kpi-title">平均里程</div>
                    <div class="kpi-value">{{ formatNumber(kpiData.avg_distance, 2) }} 英里</div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 图表区域 -->
          <div class="charts-section">
            <el-card class="chart-card">
              <template #header>
                <div class="chart-header">
                  <span>订单趋势分析</span>
                  <el-radio-group v-model="trendType" size="small">
                    <el-radio-button value="trips">订单量</el-radio-button>
                    <el-radio-button value="revenue">收入</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              <div class="chart-content">
                <LineChart v-if="trendOptions" :option="trendOptions" height="300px" />
                <el-empty v-else description="暂无趋势数据" :image-size="80" />
              </div>
            </el-card>
          </div>
        </div>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import { dashboardApi } from '@/api/dashboard'

// 组件引入
import LineChart from '@/components/charts/LineChart.vue'

// 状态定义
const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const kpiData = ref<any>(null)
const trendData = ref<any[]>([])
const dataStatus = ref<'normal' | 'updating' | 'delay'>('normal')
const lastUpdateTime = ref('2025-03-31 02:00:00')
const trendType = ref<'trips' | 'revenue'>('trips')

// 日期范围 (默认初始化 Q1 范围)
const dateRange = ref<[string, string]>(['2025-01-01', '2025-01-07'])

// 计算属性
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

// 图表配置项计算属性
const trendOptions = computed<EChartsOption | null>(() => {
  if (!trendData.value || trendData.value.length === 0) return null
  
  const xAxisData = trendData.value.map(item => item.stat_date)
  const isTrips = trendType.value === 'trips'
  
  const seriesData = trendData.value.map(item => 
    isTrips ? item.total_trips : item.total_revenue
  )

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData
    },
    yAxis: {
      type: 'value',
      name: isTrips ? '订单量(单)' : '收入(元)'
    },
    series: [
      {
        name: isTrips ? '订单量' : '收入',
        type: 'line',
        smooth: true,
        data: seriesData
      }
    ]
  }
})

// 获取数据
const fetchDashboardData = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }

  loading.value = true
  error.value = false
  
  try {
    // 并行请求 KPI 汇总和趋势图数据
    const [kpiRes, trendRes] = await Promise.all([
      dashboardApi.getKpiSummary({
        startDate: dateRange.value[0],
        endDate: dateRange.value[1]
      }),
      dashboardApi.getTrendData({
        startDate: dateRange.value[0],
        endDate: dateRange.value[1]
      })
    ])
    
    kpiData.value = kpiRes
    trendData.value = trendRes || []
    
    if (!kpiRes) {
      error.value = true
      errorMessage.value = '暂无数据'
    }
  } catch (err: any) {
    error.value = true
    errorMessage.value = err.message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

// 数字格式化函数
const formatNumber = (value: number | string, decimals: number = 2): string => {
  if (value === null || value === undefined) return '0'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return num.toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.dashboard-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.date-picker {
  width: 280px;
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

.kpi-section {
  margin: 20px 0;
}

.kpi-card {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-content {
  text-align: center;
}

.kpi-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.charts-section {
  margin: 20px 0;
}

.chart-card {
  margin-top: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-content {
  padding: 20px 0;
}
</style>
<template>
  <div class="quality-container">
    <div class="page-header">
      <h2 class="page-title">数据质量检测</h2>
      <div class="header-controls">
        <el-select v-model="selectedDate" size="small">
          <el-option label="今日" value="today" />
          <el-option label="本周" value="week" />
          <el-option label="本月" value="month" />
        </el-select>
        <el-button size="small" type="primary" @click="refreshData">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="overview-grid">
      <div class="overview-card">
        <div class="overview-header">
          <span class="overview-title">数据完整性</span>
          <span class="status-icon" :class="completenessStatus">{{ getStatusIcon(completenessStatus) }}</span>
        </div>
        <div class="overview-value">{{ dataQuality.completeness }}%</div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: dataQuality.completeness + '%' }" :class="completenessStatus"></div>
        </div>
      </div>

      <div class="overview-card">
        <div class="overview-header">
          <span class="overview-title">数据准确性</span>
          <span class="status-icon" :class="accuracyStatus">{{ getStatusIcon(accuracyStatus) }}</span>
        </div>
        <div class="overview-value">{{ dataQuality.accuracy }}%</div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: dataQuality.accuracy + '%' }" :class="accuracyStatus"></div>
        </div>
      </div>

      <div class="overview-card">
        <div class="overview-header">
          <span class="overview-title">数据一致性</span>
          <span class="status-icon" :class="consistencyStatus">{{ getStatusIcon(consistencyStatus) }}</span>
        </div>
        <div class="overview-value">{{ dataQuality.consistency }}%</div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: dataQuality.consistency + '%' }" :class="consistencyStatus"></div>
        </div>
      </div>

      <div class="overview-card">
        <div class="overview-header">
          <span class="overview-title">检测异常数</span>
          <span class="status-icon warning">⚠</span>
        </div>
        <div class="overview-value error">{{ dataQuality.anomalyCount }}</div>
        <div class="overview-desc">共发现 {{ dataQuality.anomalyCount }} 个数据异常</div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-panel">
        <div class="panel-header">
          <h3>数据质量趋势</h3>
          <span class="time-range">最近7天</span>
        </div>
        <div class="chart-wrapper">
          <v-chart :option="qualityTrendOption" autoresize />
        </div>
      </div>

      <div class="chart-panel">
        <div class="panel-header">
          <h3>异常类型分布</h3>
        </div>
        <div class="chart-wrapper">
          <v-chart :option="anomalyTypeOption" autoresize />
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-panel full-width">
        <div class="panel-header">
          <h3>异常详情列表</h3>
          <el-select v-model="filterType" size="small">
            <el-option label="全部" value="all" />
            <el-option label="缺失值" value="missing" />
            <el-option label="异常值" value="anomaly" />
            <el-option label="重复数据" value="duplicate" />
            <el-option label="格式错误" value="format" />
          </el-select>
        </div>
        <div class="table-wrapper">
          <el-table :data="filteredAnomalies" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="type" label="异常类型" width="120">
              <template #default="scope">
                <el-tag :type="getTagType(scope.row.type)">{{ getTypeName(scope.row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="tableName" label="表名" width="150" />
            <el-table-column prop="fieldName" label="字段名" width="120" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="severity" label="严重程度" width="100">
              <template #default="scope">
                <el-tag :type="getSeverityType(scope.row.severity)">{{ scope.row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detectedAt" label="检测时间" width="150" />
            <el-table-column label="操作" width="100">
              <template #default>
                <el-button size="mini" type="text">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const selectedDate = ref('today')
const filterType = ref('all')

const dataQuality = ref({
  completeness: 98.5,
  accuracy: 95.2,
  consistency: 96.8,
  anomalyCount: 12
})

const completenessStatus = computed(() => {
  if (dataQuality.value.completeness >= 95) return 'good'
  if (dataQuality.value.completeness >= 80) return 'warning'
  return 'error'
})

const accuracyStatus = computed(() => {
  if (dataQuality.value.accuracy >= 95) return 'good'
  if (dataQuality.value.accuracy >= 80) return 'warning'
  return 'error'
})

const consistencyStatus = computed(() => {
  if (dataQuality.value.consistency >= 95) return 'good'
  if (dataQuality.value.consistency >= 80) return 'warning'
  return 'error'
})

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'good': return '✓'
    case 'warning': return '⚠'
    default: return '✗'
  }
}

const days = computed(() => {
  const labels: string[] = []
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const day = new Date(now.getTime() - i * 24 * 60 * 60 * 1000)
    labels.push(`${day.getMonth() + 1}/${day.getDate()}`)
  }
  return labels
})

const qualityTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['完整性', '准确性', '一致性'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
  xAxis: { type: 'category', data: days.value },
  yAxis: { type: 'value', min: 80, max: 100 },
  series: [
    { name: '完整性', type: 'line', smooth: true, data: [97, 98, 96, 99, 98, 97, 98.5] },
    { name: '准确性', type: 'line', smooth: true, data: [94, 95, 93, 96, 95, 94, 95.2] },
    { name: '一致性', type: 'line', smooth: true, data: [96, 97, 95, 98, 97, 96, 96.8] }
  ]
}))

const anomalyTypeOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['50%', '50%'],
    data: [
      { value: 5, name: '缺失值' },
      { value: 3, name: '异常值' },
      { value: 2, name: '重复数据' },
      { value: 2, name: '格式错误' }
    ]
  }]
}))

const anomalies = ref([
  { id: 1, type: 'missing', tableName: 'trip_data', fieldName: 'passenger_count', description: '乘客数字段存在空值', severity: '中等', detectedAt: '2025-04-01 10:30:00' },
  { id: 2, type: 'anomaly', tableName: 'trip_data', fieldName: 'fare_amount', description: '费用超出正常范围', severity: '严重', detectedAt: '2025-04-01 09:15:00' },
  { id: 3, type: 'duplicate', tableName: 'payment_data', fieldName: 'transaction_id', description: '存在重复交易记录', severity: '低', detectedAt: '2025-04-01 08:45:00' },
  { id: 4, type: 'format', tableName: 'trip_data', fieldName: 'pickup_time', description: '时间格式不正确', severity: '中等', detectedAt: '2025-04-01 07:20:00' },
  { id: 5, type: 'missing', tableName: 'driver_data', fieldName: 'license_number', description: '驾驶证号码缺失', severity: '严重', detectedAt: '2025-04-01 06:00:00' }
])

const filteredAnomalies = computed(() => {
  if (filterType.value === 'all') return anomalies.value
  return anomalies.value.filter(a => a.type === filterType.value)
})

const getTagType = (type: string) => {
  const types: Record<string, string> = {
    missing: 'warning',
    anomaly: 'danger',
    duplicate: 'info',
    format: 'primary'
  }
  return types[type] || 'default'
}

const getTypeName = (type: string) => {
  const names: Record<string, string> = {
    missing: '缺失值',
    anomaly: '异常值',
    duplicate: '重复数据',
    format: '格式错误'
  }
  return names[type] || type
}

const getSeverityType = (severity: string) => {
  const types: Record<string, string> = {
    '严重': 'danger',
    '中等': 'warning',
    '低': 'info'
  }
  return types[severity] || 'default'
}

const refreshData = () => {
  console.log('Refreshing data quality...')
}
</script>

<style lang="scss" scoped>
.quality-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
  border-radius: 12px;
  color: white;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.overview-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.overview-title {
  font-size: 14px;
  color: #6b7280;
}

.status-icon {
  font-size: 20px;
  font-weight: bold;

  &.good { color: #67c23a; }
  &.warning { color: #f59e0b; }
  &.error { color: #f56c6c; }
}

.overview-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 8px;

  &.error {
    color: #f56c6c;
  }
}

.overview-desc {
  font-size: 12px;
  color: #6b7280;
}

.progress-bar {
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;

  &.good { background: linear-gradient(90deg, #67c23a, #85ce61); }
  &.warning { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
  &.error { background: linear-gradient(90deg, #f56c6c, #f87171); }
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.chart-panel.full-width {
  grid-column: span 2;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f3f4f6;
}

.panel-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.time-range {
  font-size: 12px;
  color: #6b7280;
}

.chart-wrapper {
  padding: 20px;
  height: 280px;
}

.table-wrapper {
  padding: 20px;
}
</style>

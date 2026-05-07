<template>
  <div class="quality-page">
    <el-card class="quality-card">
      <template #header>
        <div class="card-header">
          <h2>数据检测模块</h2>
          <div class="header-controls">
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="fetchQualityData"
              class="date-picker"
            />
            <el-button type="primary" @click="fetchQualityData" :loading="loading">
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

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
          <el-button type="primary" @click="fetchQualityData" class="retry-button">
            重试
          </el-button>
        </div>

        <!-- 数据展示区域 -->
        <div v-else class="data-display">
          <!-- 质量概览卡片区 -->
          <div class="overview-section">
            <el-row :gutter="20">
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="overview-card" :class="scoreClass">
                  <div class="card-content">
                    <div class="card-title">质量评分</div>
                    <div class="score-display">
                      <span class="score-value">{{ summaryData?.totalScore || 0 }}</span>
                      <el-tag size="small" :type="scoreTagType">
                        {{ scoreLevel }}
                      </el-tag>
                    </div>
                    <el-progress 
                      :percentage="Math.min(summaryData?.totalScore || 0, 100)" 
                      :color="scoreColor"
                      :stroke-width="6"
                    />
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="overview-card">
                  <div class="card-content">
                    <div class="card-title">完整率</div>
                    <div class="metric-value">{{ summaryData?.completenessRate || 0 }}%</div>
                    <el-progress 
                      :percentage="summaryData?.completenessRate || 0" 
                      type="success"
                      :stroke-width="6"
                    />
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="overview-card">
                  <div class="card-content">
                    <div class="card-title">准确率</div>
                    <div class="metric-value">{{ summaryData?.accuracyRate || 0 }}%</div>
                    <el-progress 
                      :percentage="summaryData?.accuracyRate || 0" 
                      type="warning"
                      :stroke-width="6"
                    />
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="12" :sm="6" :md="6">
                <el-card class="overview-card">
                  <div class="card-content">
                    <div class="card-title">及时性</div>
                    <div class="metric-value">{{ summaryData?.freshnessScore || 0 }}</div>
                    <el-progress 
                      :percentage="summaryData?.freshnessScore || 0" 
                      type="info"
                      :stroke-width="6"
                    />
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 核心指标区域 -->
          <div class="core-metrics-section">
            <el-row :gutter="20">
              <el-col :xs="24" :md="12">
                <el-card class="metrics-card">
                  <template #header>
                    <div class="card-header">
                      <span>表健康状态</span>
                      <el-button type="primary" size="small" @click="exportTableStatus">
                        导出
                      </el-button>
                    </div>
                  </template>
                  <div class="card-body">
                    <el-table v-if="tableHealthData.length > 0" :data="tableHealthData" stripe style="width: 100%">
                      <el-table-column prop="tableName" label="表名" width="180" />
                      <el-table-column prop="healthScore" label="健康分">
                        <template #default="scope">
                          <el-tag :type="getHealthTagType(scope.row.healthScore)">
                            {{ scope.row.healthScore }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column prop="recordCount" label="记录数" />
                      <el-table-column prop="status" label="状态">
                        <template #default="scope">
                          <el-tag :type="scope.row.status === 'normal' ? 'success' : 'danger'">
                            {{ scope.row.status === 'normal' ? '正常' : '异常' }}
                          </el-tag>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-empty v-else description="暂无表健康状态数据" :image-size="80" />
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-card class="metrics-card">
                  <template #header>
                    <div class="card-header">
                      <span>质量历史趋势</span>
                    </div>
                  </template>
                  <div class="card-body">
                    <LineChart v-if="historyChartOption" :option="historyChartOption" height="300px" />
                    <el-empty v-else description="暂无历史趋势数据" :image-size="80" />
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- 告警信息 -->
          <div class="alerts-section">
            <el-card class="alerts-card">
              <template #header>
                <div class="card-header">
                  <span>告警信息</span>
                  <el-tag v-if="alertsData.length > 0" type="danger" size="small">
                    {{ alertsData.length }}
                  </el-tag>
                </div>
              </template>
              <div class="card-body">
                <el-timeline v-if="alertsData.length > 0">
                  <el-timeline-item 
                    v-for="alert in alertsData.slice(0, 5)" 
                    :key="alert.id"
                    type="danger"
                    :timestamp="alert.timestamp"
                  >
                    {{ alert.message }}
                  </el-timeline-item>
                </el-timeline>
                <el-button v-if="alertsData.length > 5" type="text" @click="showAllAlerts">
                  查看全部告警
                </el-button>
                <el-empty v-else description="暂无告警信息" :image-size="80" />
              </div>
            </el-card>
          </div>

          <!-- 质量详情列表 -->
          <div class="detail-list-section">
            <el-card class="detail-card">
              <template #header>
                <div class="card-header">
                  <span>质量详情列表</span>
                  <el-tabs v-model="activeTab" size="small">
                    <el-tab-pane label="完整性" name="completeness" />
                    <el-tab-pane label="唯一性" name="uniqueness" />
                    <el-tab-pane label="一致性" name="consistency" />
                    <el-tab-pane label="范围" name="range" />
                    <el-tab-pane label="及时性" name="freshness" />
                  </el-tabs>
                </div>
              </template>
              <div class="card-body">
                <el-table v-if="detailTableData.length > 0" :data="detailTableData" stripe style="width: 100%">
                  <el-table-column prop="fieldName" label="字段名" width="180" />
                  <el-table-column prop="checkType" label="检测类型" />
                  <el-table-column prop="checkValue" label="检测值" />
                  <el-table-column prop="threshold" label="阈值" />
                  <el-table-column prop="status" label="状态">
                    <template #default="scope">
                      <el-tag :type="scope.row.status === 'pass' ? 'success' : 'warning'">
                        {{ scope.row.status === 'pass' ? '通过' : '警告' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-else description="暂无详情数据" :image-size="80" />
              </div>
            </el-card>
          </div>
        </div>
      </el-skeleton>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { EChartsOption } from 'echarts'
import { qualityApi } from '@/api/quality'
import LineChart from '@/components/charts/LineChart.vue'

// 状态定义
const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const selectedDate = ref<string>('2025-03-31')
const summaryData = ref<any>(null)
const tableHealthData = ref<any[]>([])
const historyChartData = ref<any[]>([])
const alertsData = ref<any[]>([])
const detailTableData = ref<any[]>([])
const activeTab = ref('completeness')

// 计算属性
const scoreClass = computed(() => {
  const score = summaryData.value?.totalScore || 0
  if (score >= 90) return 'score-excellent'
  if (score >= 70) return 'score-good'
  if (score >= 60) return 'score-warning'
  return 'score-danger'
})

const scoreLevel = computed(() => {
  const score = summaryData.value?.totalScore || 0
  if (score >= 90) return '优秀'
  if (score >= 70) return '良好'
  if (score >= 60) return '一般'
  return '较差'
})

const scoreTagType = computed(() => {
  const score = summaryData.value?.totalScore || 0
  if (score >= 90) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
})

const scoreColor = computed(() => {
  const score = summaryData.value?.totalScore || 0
  if (score >= 90) return '#67c23a'
  if (score >= 70) return '#409eff'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
})

// 图表配置项
const historyChartOption = computed<EChartsOption | null>(() => {
  if (!historyChartData.value || historyChartData.value.length === 0) return null
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
      data: historyChartData.value.map(item => item.date)
    },
    yAxis: {
      type: 'value',
      name: '评分'
    },
    series: [{
      name: '质量评分',
      type: 'line',
      smooth: true,
      data: historyChartData.value.map(item => item.score)
    }]
  }
})

// 方法
const getHealthTagType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 70) return 'primary'
  return 'danger'
}

const fetchQualityData = async () => {
  loading.value = true
  error.value = false
  
  try {
    const [reportsRes, scoreRes, alertsRes] = await Promise.all([
      qualityApi.getQualityReports({ startDate: selectedDate.value, endDate: selectedDate.value }),
      qualityApi.getDailyQualityScore({ startDate: selectedDate.value, endDate: selectedDate.value }),
      qualityApi.getAnomalyAlerts({ startDate: selectedDate.value, endDate: selectedDate.value })
    ])

    summaryData.value = reportsRes[0] || {}
    tableHealthData.value = []
    historyChartData.value = scoreRes || []
    alertsData.value = alertsRes || []

    await fetchDetailData()
  } catch (err: any) {
    error.value = true
    errorMessage.value = err.message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

const fetchDetailData = async () => {
  try {
    let res: any[] = []
    switch (activeTab.value) {
      case 'completeness':
        const nullCheckRes = await qualityApi.getNullCheckResults(selectedDate.value)
        res = Object.entries(nullCheckRes).map(([field, count]) => ({
          field,
          count
        }))
        break
      case 'uniqueness':
        const recordCheckRes = await qualityApi.getRecordCheckResults(selectedDate.value)
        res = Array.isArray(recordCheckRes) ? recordCheckRes : []
        break
      case 'consistency':
        res = []
        break
      case 'range':
        res = []
        break
      case 'freshness':
        res = []
        break
    }
    detailTableData.value = res || []
  } catch (error) {
    console.error('获取详情数据失败', error)
  }
}

const exportTableStatus = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要导出表健康状态数据吗？',
      '导出数据',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    
    ElMessage.success('导出功能已触发')
  } catch (error) {
    // 用户取消
  }
}

const showAllAlerts = () => {
  ElMessage.info('查看全部告警功能开发中')
}

// 监听标签页变化
watch(activeTab, () => {
  fetchDetailData()
})

onMounted(() => {
  fetchQualityData()
})
</script>

<style scoped>
.quality-page {
  padding: 0;
}

.quality-card {
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
  width: 180px;
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

.overview-section {
  margin: 20px 0;
}

.overview-card {
  height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-content {
  width: 100%;
  padding: 0 20px;
}

.card-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.metric-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
}

.core-metrics-section {
  margin: 20px 0;
}

.metrics-card {
  margin-bottom: 20px;
}

.card-body {
  padding: 20px 0;
}

.alerts-section {
  margin: 20px 0;
}

.alerts-card {
  margin-bottom: 20px;
}

.detail-list-section {
  margin: 20px 0;
}

.detail-card {
  margin-bottom: 20px;
}

.score-excellent {
  border-left: 4px solid #67c23a;
}

.score-good {
  border-left: 4px solid #409eff;
}

.score-warning {
  border-left: 4px solid #e6a23c;
}

.score-danger {
  border-left: 4px solid #f56c6c;
}
</style>
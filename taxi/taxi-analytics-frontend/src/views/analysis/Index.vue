<template>
  <div class="analysis-page">
    <div class="dashboard-header">
      <div class="header-left">
        <h2 class="page-title">深度分析中心</h2>
        <p class="page-subtitle">基于多维度数据的智能分析与业务洞察</p>
      </div>
      <div class="header-right">
        <div class="date-range-wrapper">
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
        </div>
        <el-button type="primary" @click="loadAllData" :loading="loading">
          执行分析
        </el-button>
      </div>
    </div>

    <el-skeleton :loading="loading" animated>
      <template #template>
        <div class="skeleton-content">
          <div class="skeleton-card"></div>
          <div class="skeleton-card"></div>
          <div class="skeleton-card"></div>
        </div>
      </template>

      <div v-if="error" class="error-section">
        <div class="error-box">
          <div class="error-title">数据加载失败</div>
          <div class="error-message">{{ errorMessage }}</div>
          <el-button type="primary" @click="loadAllData">重试</el-button>
        </div>
      </div>

      <div v-else-if="isEmptyData" class="empty-section">
        <div class="empty-box">
          <div class="empty-title">暂无数据</div>
          <div class="empty-message">请选择日期范围并点击"执行分析"按钮</div>
          <el-button type="primary" @click="loadAllData">执行分析</el-button>
        </div>
      </div>

      <div v-else class="main-content">
        <div class="analysis-tabs">
          <div 
            v-for="tab in mainTabs" 
            :key="tab.name"
            class="tab-header"
            :class="{ active: activeTab === tab.name }"
            @click="switchTab(tab.name)"
          >
            <span class="tab-label">{{ tab.label }}</span>
            <span class="tab-badge" v-if="getTabBadge(tab.name) > 0">{{ getTabBadge(tab.name) }}</span>
          </div>
        </div>

        <div class="tab-content">
          <div v-show="activeTab === 'overview'" class="overview-section">
            <div class="overview-summary">
              <div class="summary-card primary">
                <div class="summary-info">
                  <div class="summary-value primary">{{ formatNumber(totalTripCount) }}</div>
                  <div class="summary-label">总订单数</div>
                  <div class="summary-trend" v-if="trendData.length > 0">
                    <span :class="avgGrowthRate >= 0 ? 'positive' : 'negative'">
                      {{ avgGrowthRate >= 0 ? '↑' : '↓' }} {{ Math.abs(avgGrowthRate).toFixed(1) }}%
                    </span>
                    <span class="trend-label">较上期</span>
                  </div>
                </div>
              </div>
              <div class="summary-card" :class="avgGrowthRate >= 0 ? 'success' : 'negative'">
                <div class="summary-info">
                  <div class="summary-value" :class="avgGrowthRate >= 0 ? 'positive' : 'negative'">
                    {{ avgGrowthRate >= 0 ? '+' : '' }}{{ formatNumber(avgGrowthRate, 2) }}%
                  </div>
                  <div class="summary-label">平均增长率</div>
                  <div class="summary-trend">
                    <span class="trend-indicator" :class="avgGrowthRate >= 0 ? 'positive' : 'negative'">
                      {{ avgGrowthRate >= 0 ? '增长' : '下降' }}趋势
                    </span>
                  </div>
                </div>
              </div>
              <div class="summary-card warning">
                <div class="summary-info">
                  <div class="summary-value warning">{{ anomalies.length }}</div>
                  <div class="summary-label">异常检测数量</div>
                  <div class="summary-trend" v-if="anomalyRate > 0">
                    <span class="warning">{{ anomalyRate.toFixed(1) }}%</span>
                    <span class="trend-label">异常占比</span>
                  </div>
                </div>
              </div>
              <div class="summary-card success">
                <div class="summary-info">
                  <div class="summary-value success">{{ businessInsights.length }}</div>
                  <div class="summary-label">业务洞察数量</div>
                  <div class="summary-trend">
                    <span class="success">{{ generateInsightLevel(businessInsights.length) }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="overview-content">
              <div class="section">
                <h3>关键业务洞察</h3>
                <div class="insights-preview">
                  <div 
                    v-for="insight in businessInsights.slice(0, 3)" 
                    :key="insight.insightId"
                    class="insight-preview-card"
                    :class="insight.level"
                  >
                    <span class="insight-category">{{ insight.category }}</span>
                    <h4>{{ insight.title }}</h4>
                    <p>{{ insight.description }}</p>
                  </div>
                </div>
              </div>

              <div class="section">
                <h3>最新异常检测</h3>
                <div class="anomalies-preview">
                  <div 
                    v-for="anomaly in anomalies.slice(0, 3)" 
                    :key="anomaly.metricName + anomaly.anomalyDate"
                    class="anomaly-preview-card"
                    :class="anomaly.anomalyLevel"
                  >
                    <span class="anomaly-metric">{{ anomaly.metricDisplayName }}</span>
                    <span class="anomaly-date">{{ anomaly.anomalyDate }}</span>
                    <div class="anomaly-deviation" :class="anomaly.anomalyType">
                      {{ anomaly.deviationPercent }}σ 偏差
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'insights'" class="section">
            <div class="section-header">
              <h2>业务洞察</h2>
              <span class="section-badge">{{ businessInsights.length }} 条洞察</span>
            </div>
            <div class="insights-grid">
              <div 
                v-for="insight in businessInsights" 
                :key="insight.insightId"
                class="insight-card"
                :class="insight.level"
              >
                <div class="insight-header">
                  <span class="insight-category">{{ insight.category }}</span>
                  <span class="insight-level">{{ getLevelLabel(insight.level) }}</span>
                </div>
                <h3 class="insight-title">{{ insight.title }}</h3>
                <p class="insight-description">{{ insight.description }}</p>
                <div class="insight-recommendation">
                  <span class="recommendation-label">建议:</span>
                  <span class="recommendation-text">{{ insight.recommendation }}</span>
                </div>
                <div class="insight-supporting">
                  <div v-for="(data, index) in insight.supportingData" :key="index" class="supporting-item">
                    {{ data }}
                  </div>
                </div>
                <div class="insight-footer">
                  <span class="impact-score">影响评分: {{ insight.impactScore }}</span>
                  <span class="discovery-date">{{ insight.discoveryDate }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'anomaly'" class="section">
            <div class="section-header">
              <h2>异常检测与根因分析</h2>
              <span class="section-badge">{{ anomalies.length }} 个异常</span>
            </div>
            <div class="anomaly-list">
              <div 
                v-for="anomaly in anomalies" 
                :key="anomaly.metricName + anomaly.anomalyDate"
                class="anomaly-card"
                :class="anomaly.anomalyLevel"
              >
                <div class="anomaly-header">
                  <span class="anomaly-metric">{{ anomaly.metricDisplayName }}</span>
                  <span class="anomaly-date">{{ anomaly.anomalyDate }}</span>
                </div>
                <div class="anomaly-content">
                  <div class="anomaly-description">{{ anomaly.description }}</div>
                  <div class="anomaly-values">
                    <div class="value-item">
                      <span class="value-label">实际值</span>
                      <span class="value-actual">{{ formatNumber(anomaly.actualValue) }}</span>
                    </div>
                    <div class="value-item">
                      <span class="value-label">预期值</span>
                      <span class="value-expected">{{ formatNumber(anomaly.expectedValue) }}</span>
                    </div>
                    <div class="value-item">
                      <span class="value-label">偏差</span>
                      <span class="value-deviation" :class="anomaly.anomalyType">{{ anomaly.deviationPercent }}σ</span>
                    </div>
                  </div>
                </div>
                <div class="anomaly-causes">
                  <div class="causes-header">可能原因</div>
                  <div class="causes-list">
                    <span v-for="(cause, index) in anomaly.potentialCauses" :key="index" class="cause-tag">
                      {{ cause }}
                    </span>
                  </div>
                </div>
                <div v-if="anomaly.rootCauses && anomaly.rootCauses.length > 0" class="anomaly-root-causes">
                  <div class="root-causes-header">根因分析</div>
                  <div class="root-causes-list">
                    <div v-for="(rootCause, index) in anomaly.rootCauses" :key="index" class="root-cause-item">
                      <span class="root-dimension">{{ rootCause.dimension }}:</span>
                      <span class="root-value">{{ rootCause.value }}</span>
                      <span class="root-contribution">贡献度 {{ rootCause.contribution }}%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'prediction'" class="section">
            <div class="section-header">
              <h2>需求预测分析</h2>
              <span class="section-badge">未来 {{ predictionDays }} 天预测</span>
            </div>
            <div class="prediction-content">
              <div class="prediction-chart-container">
                <div ref="predictionChartRef" class="chart-area"></div>
              </div>
              <div class="prediction-table">
                <table>
                  <thead>
                    <tr>
                      <th>日期</th>
                      <th>预测订单数</th>
                      <th>置信区间</th>
                      <th>趋势</th>
                      <th>置信度</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="prediction in predictions" :key="prediction.date">
                      <td>{{ prediction.date }}</td>
                      <td class="predicted-value">{{ formatNumber(prediction.predictedValue) }}</td>
                      <td class="confidence-interval">{{ formatNumber(prediction.lowerBound) }} - {{ formatNumber(prediction.upperBound) }}</td>
                      <td :class="prediction.trendDirection">
                        {{ prediction.trendDirection === 'up' ? '↑' : '↓' }} {{ formatNumber(prediction.trend, 2) }}%
                      </td>
                      <td>{{ (prediction.confidence * 100).toFixed(0) }}%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'multiDimension'" class="section">
            <MultiDimensionAnalysis
              :start-date="dateRange[0]"
              :end-date="dateRange[1]"
            />
          </div>

          <div v-show="activeTab === 'trend'" class="section">
            <div class="section-header">
              <h2>趋势分析</h2>
              <span class="section-badge">{{ trendData.length }} 天数据</span>
            </div>
            <div class="trend-content">
              <div class="trend-chart-container">
                <div ref="trendChartRef" class="chart-area"></div>
              </div>
              <div class="trend-summary">
                <div class="trend-stat-item primary">
                  <div class="trend-stat-header">
                    <span class="trend-stat-label">总订单数</span>
                    <span class="trend-stat-badge" :class="getOrderLevel(totalTripCount).class">
                      {{ getOrderLevel(totalTripCount).label }}
                    </span>
                  </div>
                  <div class="trend-stat-value primary">{{ formatNumber(totalTripCount) }}</div>
                  <div class="trend-stat-description">{{ generateTripCountDescription(totalTripCount, trendData.length) }}</div>
                </div>
                <div class="trend-stat-item" :class="getDailyLevel(avgDailyTrips).class">
                  <div class="trend-stat-header">
                    <span class="trend-stat-label">平均日订单</span>
                    <span class="trend-stat-badge" :class="getDailyLevel(avgDailyTrips).class">
                      {{ getDailyLevel(avgDailyTrips).label }}
                    </span>
                  </div>
                  <div class="trend-stat-value" :class="getDailyLevel(avgDailyTrips).class">{{ formatNumber(avgDailyTrips) }}</div>
                  <div class="trend-stat-description">{{ generateDailyTripsDescription(avgDailyTrips) }}</div>
                </div>
                <div class="trend-stat-item growth">
                  <div class="trend-stat-header">
                    <span class="trend-stat-label">平均增长率</span>
                    <span class="trend-stat-badge" :class="avgGrowthRate >= 0 ? 'positive' : 'negative'">
                      {{ avgGrowthRate >= 0 ? '增长' : '下降' }}
                    </span>
                  </div>
                  <div class="trend-stat-value" :class="avgGrowthRate >= 0 ? 'positive' : 'negative'">
                    {{ avgGrowthRate >= 0 ? '+' : '' }}{{ formatNumber(avgGrowthRate, 2) }}%
                  </div>
                  <div class="trend-stat-description">{{ generateGrowthRateDescription(avgGrowthRate) }}</div>
                </div>
                <div class="trend-stat-item amount">
                  <div class="trend-stat-header">
                    <span class="trend-stat-label">平均金额</span>
                    <span class="trend-stat-badge" :class="getAmountLevel(avgAmount).class">
                      {{ getAmountLevel(avgAmount).label }}
                    </span>
                  </div>
                  <div class="trend-stat-value amount">${{ formatNumber(avgAmount, 2) }}</div>
                  <div class="trend-stat-description">{{ generateAvgAmountDescription(avgAmount) }}</div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="activeTab === 'basic'" class="section">
            <BasicAnalysis
              :start-date="dateRange[0]"
              :end-date="dateRange[1]"
            />
          </div>
        </div>
      </div>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { analysisApi } from '@/api/analysis'
import BasicAnalysis from '@/components/analysis/BasicAnalysis.vue'
import MultiDimensionAnalysis from '@/components/analysis/MultiDimensionAnalysis.vue'
import type {
  AirportStatistics,
  VendorComparison,
  PaymentDistribution,
  DistanceDistribution,
  DurationDistribution,
  PassengerDistribution,
  TipDistribution,
  MultiDimensionAnalysisDTO,
  AnomalyDetectionDTO,
  PredictionDTO,
  BusinessInsightDTO,
  TrendAnalysisDTO
} from '@/api/analysis'
import * as echarts from 'echarts'

const loading = ref(false)
const error = ref(false)
const errorMessage = ref('')
const dateRange = ref<[string, string]>(['2025-01-01', '2025-01-31'])
const lastUpdateTime = ref('')
const predictionDays = ref(7)
const dimension1 = ref('vendor')
const dimension2 = ref('payment')
const activeTab = ref(localStorage.getItem('analysisActiveTab') || 'overview')

const mainTabs = [
  { name: 'overview', label: '分析概览' },
  { name: 'insights', label: '业务洞察' },
  { name: 'anomaly', label: '异常检测' },
  { name: 'prediction', label: '需求预测' },
  { name: 'multiDimension', label: '多维交叉分析' },
  { name: 'trend', label: '趋势分析' },
  { name: 'basic', label: '基础业务分析' }
]

const airportStats = ref<AirportStatistics[]>([])
const vendorStats = ref<VendorComparison[]>([])
const paymentStats = ref<PaymentDistribution[]>([])
const distanceStats = ref<DistanceDistribution[]>([])
const durationStats = ref<DurationDistribution[]>([])
const passengerStats = ref<PassengerDistribution[]>([])
const tipStats = ref<TipDistribution[]>([])
const multiDimensionData = ref<MultiDimensionAnalysisDTO[]>([])
const anomalies = ref<AnomalyDetectionDTO[]>([])
const predictions = ref<PredictionDTO[]>([])
const businessInsights = ref<BusinessInsightDTO[]>([])
const trendData = ref<TrendAnalysisDTO[]>([])

const predictionChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
const multiDimensionBarChartRef = ref<HTMLElement | null>(null)
const multiDimensionPieChartRef = ref<HTMLElement | null>(null)

let predictionChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let multiDimensionBarChart: echarts.ECharts | null = null
let multiDimensionPieChart: echarts.ECharts | null = null

const isEmptyData = computed(() => {
  return airportStats.value.length === 0 &&
         vendorStats.value.length === 0 &&
         paymentStats.value.length === 0 &&
         businessInsights.value.length === 0
})

const kpiSummary = ref<{ trip_count: number; total_revenue: number; avg_fare: number; avg_distance: number } | null>(null)

const totalTripCount = computed(() => {
  if (kpiSummary.value) {
    return kpiSummary.value.trip_count
  }
  return trendData.value.reduce((sum, item) => sum + item.tripCount, 0)
})

const avgDailyTrips = computed(() => {
  if (trendData.value.length === 0) return 0
  return Math.round(totalTripCount.value / trendData.value.length)
})

const avgGrowthRate = computed(() => {
  const rates = trendData.value.filter(item => item.growthRate !== undefined && item.growthRate !== null)
  if (rates.length === 0) return 0
  return rates.reduce((sum, item) => sum + (item.growthRate || 0), 0) / rates.length
})

const avgAmount = computed(() => {
  const amounts = trendData.value.filter(item => item.avgAmount > 0)
  if (amounts.length === 0) return 0
  return amounts.reduce((sum, item) => sum + item.avgAmount, 0) / amounts.length
})

const anomalyRate = computed(() => {
  if (totalTripCount.value === 0) return 0
  return (anomalies.value.length / totalTripCount.value) * 100
})

const generateInsightLevel = (count: number): string => {
  if (count === 0) return '暂无洞察'
  if (count <= 3) return '洞察较少'
  if (count <= 6) return '洞察适中'
  if (count <= 10) return '洞察丰富'
  return '洞察非常丰富'
}

const getOrderLevel = (total: number) => {
  if (total < 10000) return { label: '较低', class: 'warning' }
  if (total < 100000) return { label: '中等', class: 'info' }
  if (total < 1000000) return { label: '较高', class: 'positive' }
  return { label: '很高', class: 'success' }
}

const getDailyLevel = (daily: number) => {
  if (daily < 100) return { label: '低迷', class: 'negative' }
  if (daily < 1000) return { label: '平稳', class: 'info' }
  if (daily < 10000) return { label: '活跃', class: 'positive' }
  return { label: '火爆', class: 'success' }
}

const getAmountLevel = (amount: number) => {
  if (amount < 10) return { label: '偏低', class: 'warning' }
  if (amount < 20) return { label: '正常', class: 'info' }
  if (amount < 50) return { label: '较高', class: 'positive' }
  return { label: '高', class: 'success' }
}

const generateTripCountDescription = (total: number, days: number): string => {
  const daily = Math.round(total / days)
  
  if (total < 10000) {
    return `分析周期 ${days} 天内共 ${formatNumber(total)} 笔订单，日均 ${formatNumber(daily)} 单。当前业务规模较小，建议通过促销活动、优化服务质量提升订单量。预测若增长率保持当前水平，${Math.ceil((10000 - total) / daily)} 天后可突破万单规模。`
  } else if (total < 100000) {
    return `分析周期 ${days} 天内共 ${formatNumber(total)} 笔订单，日均 ${formatNumber(daily)} 单。业务处于中等规模阶段，建议深耕现有客户群体，提升复购率。根据历史数据预测，若保持现有增长趋势，预计 ${Math.ceil((100000 - total) / daily)} 天后可达到十万级规模。`
  } else if (total < 1000000) {
    const avgOrderValue = avgAmount.value > 0 ? `，客单价约 $${avgAmount.value.toFixed(2)}` : ''
    return `分析周期 ${days} 天内共 ${formatNumber(total)} 笔订单，日均 ${formatNumber(daily)} 单${avgOrderValue}。业务规模较大，建议关注运营效率优化。预测下周期订单量有望突破 ${formatNumber(Math.round(total * 1.1))} 单。`
  } else {
    const marketShare = (total / 5000000 * 100).toFixed(1)
    return `分析周期 ${days} 天内共 ${formatNumber(total)} 笔订单，日均 ${formatNumber(daily)} 单。业务规模庞大，市场占有率约 ${marketShare}%。建议拓展新业务线，探索增值服务提升收入。`
  }
}

const generateDailyTripsDescription = (daily: number): string => {
  const growthRate = avgGrowthRate.value
  
  if (daily < 100) {
    const neededDays = Math.ceil((100 - daily) / daily * 100 / Math.abs(growthRate || 1))
    return `日均订单量 ${formatNumber(daily)} 单，处于低迷状态。建议分析竞争对手动态，优化定价策略，改善用户体验。若能实现每日 ${(daily * 0.05).toFixed(0)} 单的增长，预计 ${neededDays} 天后可达到日均百单水平。`
  } else if (daily < 1000) {
    const targetDays = Math.ceil((1000 - daily) / daily * 100 / (growthRate || 2))
    return `日均订单量 ${formatNumber(daily)} 单，运营平稳。建议通过会员体系、积分奖励等方式提升用户粘性。根据当前增长趋势${growthRate > 0 ? '' : '（若能扭转趋势）'}，预计 ${targetDays} 天后可突破日均千单。`
  } else if (daily < 10000) {
    const weeklyGrowth = daily * 7 * (growthRate / 100 || 0.02)
    return `日均订单量 ${formatNumber(daily)} 单，业务活跃。建议加大营销投入，拓展新市场。预计下周订单量将增加约 ${formatNumber(Math.round(weeklyGrowth))} 单。`
  } else {
    return `日均订单量 ${formatNumber(daily)} 单，业务火爆。需重点关注服务质量和运营稳定性，避免因单量激增导致用户体验下降。建议考虑峰值时段的运力调配优化。`
  }
}

const generateGrowthRateDescription = (rate: number): string => {
  const confidence = trendData.value.length >= 14 ? '较高' : '一般'
  
  if (rate < -5) {
    const recoveryTarget = Math.abs(rate) + 5
    return `订单量呈显著下降趋势，降幅 ${Math.abs(rate).toFixed(2)}%，数据置信度${confidence}。需立即分析下降原因，建议检查市场环境变化、竞争对手动态。预测若不采取措施，下月订单量可能继续下降 ${(rate * 1.5).toFixed(1)}%。建议制定紧急促销方案，目标在 ${Math.ceil(recoveryTarget / Math.abs(rate))} 个周期内恢复正增长。`
  } else if (rate < 0) {
    return `订单量略有下降，降幅 ${Math.abs(rate).toFixed(2)}%，数据置信度${confidence}。建议优化营销策略，加强客户关怀。预测通过针对性措施，有望在1-2个周期内实现正增长。`
  } else if (rate < 5) {
    const targetRate = 5
    const accelerationTarget = targetRate - rate
    return `订单量保持稳定，增长率 ${rate.toFixed(2)}%，数据置信度${confidence}。建议通过创新服务、拓展渠道等方式寻求突破。若能提升增长率 ${accelerationTarget.toFixed(2)} 个百分点，达到 ${targetRate.toFixed(2)}%，有望进入快速增长通道。`
  } else if (rate < 15) {
    const doublingTime = Math.log(2) / Math.log(1 + rate / 100)
    return `订单量呈良好增长态势，增长率 ${rate.toFixed(2)}%，数据置信度${confidence}，表现优秀！预计约 ${doublingTime.toFixed(1)} 个周期后订单量将翻倍。建议加大投入，乘势扩张市场份额。`
  } else {
    const monthlyProjection = totalTripCount.value * (1 + rate / 100)
    return `订单量高速增长，增长率达 ${rate.toFixed(2)}%，数据置信度${confidence}，业务发展势头强劲！预测下一周期订单量将达 ${formatNumber(Math.round(monthlyProjection))} 单。建议关注运营能力匹配，确保服务质量不随扩张下降。`
  }
}

const generateAvgAmountDescription = (amount: number): string => {
  const highValueRatio = avgAmountPerMile.value > 3 ? '较高' : '正常'
  
  if (amount < 10) {
    const improvementTarget = (10 - amount) / amount * 100
    return `平均订单金额 $${amount.toFixed(2)}，处于较低水平。建议推出高端服务套餐、优化定价策略提升客单价。若能提升 ${improvementTarget.toFixed(0)}%，可显著改善盈利能力。同时需关注每英里均价 ${avgAmountPerMile.value.toFixed(2)} 美元，评估定价合理性。`
  } else if (amount < 20) {
    return `平均订单金额 $${amount.toFixed(2)}，处于行业平均水平。建议通过增值服务（如优先派单、豪华车型）提升高价值订单占比。每英里均价 ${avgAmountPerMile.value.toFixed(2)} 美元，${highValueRatio}，可进一步优化定价结构。`
  } else if (amount < 50) {
    const premiumRatio = (amount / 20 * 100 - 100).toFixed(0)
    return `平均订单金额 $${amount.toFixed(2)}，较高，盈利能力较强。高价值订单占比较同行高出约 ${premiumRatio}%。建议继续深耕高端市场，同时关注成本控制。预测通过精细化运营，客单价有望提升至 $${(amount * 1.1).toFixed(2)}。`
  } else {
    const luxuryShare = (amount / 30 * 100).toFixed(0)
    return `平均订单金额 $${amount.toFixed(2)}，非常高，高端订单占比大。预计豪华订单占比约 ${luxuryShare}%。建议加强高端客户服务体验，维护品牌形象。需注意高端市场饱和度，适时拓展大众市场实现多元化发展。`
  }
}

const avgAmountPerMile = computed(() => {
  if (trendData.value.length === 0) return 0
  
  let totalAmount = 0
  let totalDistance = 0
  let validCount = 0
  
  trendData.value.forEach(item => {
    if (item.avgAmount > 0 && item.avgDistance > 0) {
      totalAmount += item.avgAmount
      totalDistance += item.avgDistance
      validCount++
    }
  })
  
  if (validCount === 0) return 0
  
  const avgAmount = totalAmount / validCount
  const avgDistance = totalDistance / validCount
  
  return avgDistance > 0 ? avgAmount / avgDistance : 0
})

const currentMultiDimensionData = computed(() => {
  return multiDimensionData.value
})

const totalMultiDimensionAmount = computed(() => {
  return multiDimensionData.value.reduce((sum, item) => sum + item.totalAmount, 0)
})



const formatNumber = (num: number | undefined, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

const getLevelLabel = (level: string): string => {
  const levelMap: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低',
    critical: '严重'
  }
  return levelMap[level] || level
}

const getTabBadge = (tabName: string): number => {
  const badgeMap: Record<string, number> = {
    insights: businessInsights.value.length,
    anomaly: anomalies.value.length
  }
  return badgeMap[tabName] || 0
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
      analysisApi.getDurationDistribution({ startDate, endDate }),
      analysisApi.getPassengerDistribution({ startDate, endDate }),
      analysisApi.getTipDistribution({ startDate, endDate }),
      analysisApi.getMultiDimensionAnalysis({ startDate, endDate, dimension1: dimension1.value, dimension2: dimension2.value }),
      analysisApi.detectAnomalies({ startDate, endDate }),
      analysisApi.getPredictions({ startDate, endDate, days: predictionDays.value }),
      analysisApi.generateBusinessInsights({ startDate, endDate }),
      analysisApi.getTrendAnalysis({ startDate, endDate }),
      analysisApi.getKpiSummary({ startDate, endDate })
    ])

    airportStats.value = results[0] || []
    vendorStats.value = results[1] || []
    paymentStats.value = results[2] || []
    distanceStats.value = results[3] || []
    durationStats.value = results[4] || []
    passengerStats.value = results[5] || []
    tipStats.value = results[6] || []
    multiDimensionData.value = results[7] || []
    anomalies.value = results[8] || []
    predictions.value = results[9] || []
    businessInsights.value = results[10] || []
    trendData.value = results[11] || []
    kpiSummary.value = results[12] || null

    lastUpdateTime.value = new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })

    await nextTick()
    renderCurrentTabCharts()
  } catch (err: any) {
    error.value = true
    errorMessage.value = err.message || '数据加载失败'
  } finally {
    loading.value = false
  }
}

const switchTab = async (tabName: string) => {
  activeTab.value = tabName
  localStorage.setItem('analysisActiveTab', tabName)
  await nextTick()
  renderCurrentTabCharts()
}

const renderCurrentTabCharts = () => {
  disposeAllCharts()
  
  switch (activeTab.value) {
    case 'prediction':
      renderPredictionChart()
      break
    case 'trend':
      renderTrendChart()
      break
    case 'multiDimension':
      renderMultiDimensionCharts()
      break
  }
}

const disposeAllCharts = () => {
  predictionChart?.dispose()
  predictionChart = null
  trendChart?.dispose()
  trendChart = null
  multiDimensionBarChart?.dispose()
  multiDimensionBarChart = null
  multiDimensionPieChart?.dispose()
  multiDimensionPieChart = null
}

const ensureChartContainer = (refEl: HTMLElement | null): boolean => {
  if (!refEl) return false
  const rect = refEl.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

const renderPredictionChart = () => {
  if (!ensureChartContainer(predictionChartRef.value) || predictions.value.length === 0) return

  try {
    predictionChart = echarts.init(predictionChartRef.value!)

    const option: echarts.EChartsOption = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['预测值', '置信区间'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: predictions.value.map(p => p.date)
      },
      yAxis: { type: 'value', name: '订单数' },
      series: [
        {
          name: '预测值',
          type: 'line',
          data: predictions.value.map(p => p.predictedValue),
          smooth: true,
          lineStyle: { width: 3, color: '#3b82f6' },
          itemStyle: { color: '#3b82f6' },
          symbol: 'circle',
          symbolSize: 8
        },
        {
          name: '置信区间',
          type: 'line',
          data: predictions.value.map(p => p.lowerBound),
          smooth: true,
          lineStyle: { width: 1, type: 'dashed', color: '#909399' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0)' }
            ])
          },
          symbol: 'none'
        },
        {
          name: '置信区间',
          type: 'line',
          data: predictions.value.map(p => p.upperBound),
          smooth: true,
          lineStyle: { width: 1, type: 'dashed', color: '#909399' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0)' }
            ])
          },
          symbol: 'none'
        }
      ]
    }

    predictionChart.setOption(option)
  } catch (err) {
    console.error('Failed to render prediction chart:', err)
  }
}



const renderTrendChart = () => {
  if (!ensureChartContainer(trendChartRef.value) || trendData.value.length === 0) return

  try {
    trendChart = echarts.init(trendChartRef.value!)
    
    const growthRates = trendData.value.map(t => t.growthRate).filter(v => v !== undefined && v !== null)
    const minGrowth = Math.min(...growthRates, 0) - 5
    const maxGrowth = Math.max(...growthRates, 0) + 5

    const option: echarts.EChartsOption = {
      tooltip: { 
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: any) => {
          let result = `${params[0].axisValue}<br/>`
          params.forEach((param: any) => {
            const marker = param.marker
            const name = param.seriesName
            let value = param.value
            if (name === '增长率') {
              value = `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
            } else if (name === '订单数' || name === '移动平均') {
              value = formatNumber(value)
            }
            result += `${marker} ${name}: ${value}<br/>`
          })
          return result
        }
      },
      legend: { 
        data: ['订单数', '移动平均', '增长率'],
        top: 0
      },
      grid: { left: '3%', right: '4%', bottom: '8%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trendData.value.map(t => t.date),
        axisLabel: { rotate: trendData.value.length > 14 ? 45 : 0 }
      },
      yAxis: [
        { 
          type: 'value', 
          name: '订单数', 
          position: 'left',
          axisLabel: { formatter: (value: number) => formatNumber(value) }
        },
        { 
          type: 'value', 
          name: '增长率(%)', 
          position: 'right',
          min: minGrowth,
          max: maxGrowth,
          axisLabel: { formatter: (value: number) => `${value >= 0 ? '+' : ''}${value.toFixed(1)}%` }
        }
      ],
      series: [
        {
          name: '订单数',
          type: 'bar',
          data: trendData.value.map(t => t.tripCount),
          itemStyle: { color: '#3b82f6' },
          barMaxWidth: 30
        },
        {
          name: '移动平均',
          type: 'line',
          data: trendData.value.map(t => t.movingAverage),
          smooth: true,
          lineStyle: { width: 3, color: '#10b981' },
          itemStyle: { color: '#10b981' },
          symbol: 'circle',
          symbolSize: 6
        },
        {
          name: '增长率',
          type: 'line',
          yAxisIndex: 1,
          data: trendData.value.map(t => t.growthRate),
          smooth: true,
          lineStyle: { width: 2, color: '#ef4444' },
          itemStyle: { color: '#ef4444' },
          symbol: 'circle',
          symbolSize: 5,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(239, 68, 68, 0.25)' },
              { offset: 1, color: 'rgba(239, 68, 68, 0)' }
            ])
          }
        }
      ]
    }

    trendChart.setOption(option)
  } catch (err) {
    console.error('Failed to render trend chart:', err)
  }
}

const renderMultiDimensionCharts = () => {
  renderMultiDimensionBarChart()
  renderMultiDimensionPieChart()
}

const renderMultiDimensionBarChart = () => {
  if (!ensureChartContainer(multiDimensionBarChartRef.value) || currentMultiDimensionData.value.length === 0) return

  try {
    multiDimensionBarChart = echarts.init(multiDimensionBarChartRef.value!)

    const topData = currentMultiDimensionData.value.slice(0, 10)
    const labels = topData.map(item => `${item.dimension1Name || item.dimension1} × ${item.dimension2Name || item.dimension2}`)
    const tripCounts = topData.map(item => item.tripCount)
    const amounts = topData.map(item => item.totalAmount)

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          const barData = params[0]
          const lineData = params[1]
          return `${barData.name}<br/>订单数: ${formatNumber(barData.value)}<br/>总金额: $${formatNumber(lineData.value, 2)}`
        }
      },
      legend: { data: ['订单数', '总金额(千$)'], top: 0 },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { rotate: 45, fontSize: 11 }
      },
      yAxis: [
        { type: 'value', name: '订单数', position: 'left' },
        { type: 'value', name: '金额(千$)', position: 'right', axisLabel: { formatter: (v: number) => (v / 1000).toFixed(1) } }
      ],
      series: [
        {
          name: '订单数',
          type: 'bar',
          data: tripCounts,
          itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 40
        },
        {
          name: '总金额(千$)',
          type: 'line',
          yAxisIndex: 1,
          data: amounts,
          smooth: true,
          lineStyle: { width: 3, color: '#10b981' },
          itemStyle: { color: '#10b981' },
          symbol: 'circle',
          symbolSize: 8
        }
      ]
    }

    multiDimensionBarChart.setOption(option)
  } catch (err) {
    console.error('Failed to render multi-dimension bar chart:', err)
  }
}

const renderMultiDimensionPieChart = () => {
  if (!ensureChartContainer(multiDimensionPieChartRef.value) || currentMultiDimensionData.value.length === 0) return

  try {
    multiDimensionPieChart = echarts.init(multiDimensionPieChartRef.value!)

    const topData = currentMultiDimensionData.value.slice(0, 10)
    const pieData = topData.map(item => ({
      name: `${item.dimension1Name || item.dimension1} × ${item.dimension2Name || item.dimension2}`,
      value: item.totalAmount
    }))

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          const percent = ((params.value / totalMultiDimensionAmount.value) * 100).toFixed(1)
          return `${params.name}<br/>金额: $${formatNumber(params.value, 2)} (${percent}%)`
        }
      },
      legend: {
        orient: 'vertical',
        right: '5%',
        top: 'center',
        itemWidth: 12,
        itemHeight: 12,
        textStyle: { fontSize: 11 }
      },
      series: [{
        name: '金额分布',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '55%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%', fontSize: 11 },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
        data: pieData
      }]
    }

    multiDimensionPieChart.setOption(option)
  } catch (err) {
    console.error('Failed to render multi-dimension pie chart:', err)
  }
}

const handleResize = () => {
  predictionChart?.resize()
  trendChart?.resize()
  multiDimensionBarChart?.resize()
  multiDimensionPieChart?.resize()
}

onMounted(() => {
  loadAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  disposeAllCharts()
})
</script>

<style scoped lang="scss">
.analysis-page {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}

.header-left {
  flex: 1;
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

.date-range-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;

  .date-picker {
    width: 280px;
  }
}

.error-section,
.empty-section {
  display: flex;
  justify-content: center;
  padding: 60px 0;

  .error-box,
  .empty-box {
    text-align: center;
    padding: 40px;
    background-color: #ffffff;
    border-radius: 8px;

    .error-title,
    .empty-title {
      font-size: 18px;
      font-weight: 600;
      color: #374151;
      margin-bottom: 8px;
    }

    .error-message,
    .empty-message {
      font-size: 14px;
      color: #6b7280;
      margin-bottom: 20px;
    }
  }
}

.main-content {
  .analysis-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 24px;
    padding: 8px;
    background-color: #ffffff;
    border-radius: 8px;
    overflow-x: auto;

    .tab-header {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 12px 20px;
      font-size: 14px;
      color: #6b7280;
      cursor: pointer;
      border-radius: 6px;
      transition: all 0.2s;
      white-space: nowrap;

      &:hover {
        background-color: #f3f4f6;
      }

      &.active {
        background-color: #3b82f6;
        color: #ffffff;

        .tab-badge {
          background-color: rgba(255, 255, 255, 0.3);
        }
      }

      .tab-badge {
        padding: 2px 8px;
        background-color: #e5e7eb;
        border-radius: 10px;
        font-size: 12px;
        font-weight: 500;
      }
    }
  }

  .tab-content {
    animation: fadeIn 0.3s ease;
  }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.overview-section {
  .overview-summary {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 24px;

    .summary-card {
      padding: 24px 20px;
      background-color: #ffffff;
      border-radius: 12px;
      border: 1px solid #e5e7eb;
      text-align: center;
      transition: all 0.2s ease;

      &:hover {
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
      }

      &.primary {
        border-color: #3b82f6;
        background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
        
        .summary-value.primary {
          color: #3b82f6;
        }
      }

      &.warning {
        border-color: #f59e0b;
        background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
        
        .summary-value.warning {
          color: #f59e0b;
        }
      }

      &.success {
        border-color: #10b981;
        background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
        
        .summary-value.success {
          color: #10b981;
        }
      }

      &.negative {
        border-color: #ef4444;
        background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
        
        .summary-value.negative {
          color: #ef4444;
        }
      }

      .summary-info {
        .summary-value {
          font-size: 32px;
          font-weight: 700;
          color: #1f2937;
          display: block;
          line-height: 1.2;
        }

        .summary-label {
          font-size: 13px;
          color: #6b7280;
          margin-top: 8px;
          display: block;
          font-weight: 500;
        }

        .summary-trend {
          margin-top: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;

          .trend-label {
            font-size: 11px;
            color: #9ca3af;
          }

          .trend-indicator {
            font-size: 11px;
            font-weight: 600;
          }
        }

        .positive { color: #10b981; }
        .negative { color: #ef4444; }
        .warning { color: #f59e0b; }
        .success { color: #10b981; }
      }
    }
  }

  .overview-content {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;

    .section {
      background-color: #ffffff;
      border-radius: 8px;
      padding: 20px;

      h3 {
        margin: 0 0 16px 0;
        font-size: 16px;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .insights-preview {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .insight-preview-card {
        padding: 16px;
        border-radius: 8px;
        border-left: 4px solid;

        &.high { border-color: #ef4444; background-color: #fef2f2; }
        &.medium { border-color: #f59e0b; background-color: #fffbeb; }
        &.low { border-color: #3b82f6; background-color: #eff6ff; }

        .insight-category {
          font-size: 12px;
          color: #6b7280;
          padding: 2px 8px;
          background-color: rgba(0, 0, 0, 0.05);
          border-radius: 4px;
        }

        h4 {
          margin: 8px 0 4px 0;
          font-size: 14px;
          font-weight: 600;
          color: #1f2937;
        }

        p {
          margin: 0;
          font-size: 13px;
          color: #6b7280;
          line-height: 1.4;
        }
      }
    }

    .anomalies-preview {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .anomaly-preview-card {
        padding: 16px;
        border-radius: 8px;
        border-left: 4px solid;
        display: flex;
        align-items: center;
        gap: 12px;

        &.critical { border-color: #dc2626; background-color: #fef2f2; }
        &.high { border-color: #ef4444; background-color: #fef2f2; }
        &.medium { border-color: #f59e0b; background-color: #fffbeb; }

        .anomaly-metric {
          font-size: 14px;
          font-weight: 600;
          color: #1f2937;
        }

        .anomaly-date {
          font-size: 12px;
          color: #6b7280;
        }

        .anomaly-deviation {
          margin-left: auto;
          font-size: 13px;
          font-weight: 600;
          padding: 4px 10px;
          border-radius: 4px;

          &.spike { color: #ef4444; background-color: #fee2e2; }
          &.drop { color: #3b82f6; background-color: #dbeafe; }
        }
      }
    }
  }
}

.section {
  background-color: #ffffff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #1f2937;
    }

    .section-badge {
      padding: 4px 12px;
      background-color: #e0f2fe;
      color: #0369a1;
      font-size: 12px;
      border-radius: 20px;
    }

    .dimension-selectors {
      display: flex;
      align-items: center;
      gap: 8px;

      .dimension-select {
        padding: 6px 12px;
        border: 1px solid #d1d5db;
        border-radius: 4px;
        font-size: 14px;
        background-color: #ffffff;
        cursor: pointer;
        min-width: 100px;

        &:hover {
          border-color: #3b82f6;
        }
      }

      .dimension-separator {
        font-size: 18px;
        color: #9ca3af;
      }

      .chart-type-separator {
        font-size: 14px;
        color: #d1d5db;
        margin-left: 4px;
      }
    }
  }
}

.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;

  .insight-card {
    padding: 20px;
    border-radius: 8px;
    border-left: 4px solid;

    &.high { border-color: #ef4444; background-color: #fef2f2; }
    &.medium { border-color: #f59e0b; background-color: #fffbeb; }
    &.low { border-color: #3b82f6; background-color: #eff6ff; }
    &.critical { border-color: #dc2626; background-color: #fef2f2; }

    .insight-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;

      .insight-category {
        font-size: 12px;
        color: #6b7280;
        padding: 2px 8px;
        background-color: rgba(0, 0, 0, 0.05);
        border-radius: 4px;
      }

      .insight-level {
        font-size: 12px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 4px;

        .insight-card.high & { color: #ef4444; background-color: #fee2e2; }
        .insight-card.medium & { color: #f59e0b; background-color: #fef3c7; }
        .insight-card.low & { color: #3b82f6; background-color: #dbeafe; }
        .insight-card.critical & { color: #dc2626; background-color: #fee2e2; }
      }
    }

    .insight-title {
      margin: 0 0 8px 0;
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    .insight-description {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #4b5563;
      line-height: 1.5;
    }

    .insight-recommendation {
      margin-bottom: 12px;
      padding: 12px;
      background-color: rgba(0, 0, 0, 0.03);
      border-radius: 4px;

      .recommendation-label {
        font-size: 12px;
        color: #6b7280;
        font-weight: 500;
      }

      .recommendation-text {
        font-size: 14px;
        color: #374151;
        margin-left: 8px;
      }
    }

    .insight-supporting {
      margin-bottom: 12px;

      .supporting-item {
        font-size: 12px;
        color: #6b7280;
        padding: 4px 0;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
      }
    }

    .insight-footer {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #9ca3af;

      .impact-score {
        font-weight: 500;
      }
    }
  }
}

.anomaly-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(450px, 1fr));
  gap: 20px;

  .anomaly-card {
    padding: 20px;
    border-radius: 8px;
    border-left: 4px solid;

    &.critical { border-color: #dc2626; background-color: #fef2f2; }
    &.high { border-color: #ef4444; background-color: #fef2f2; }
    &.medium { border-color: #f59e0b; background-color: #fffbeb; }

    .anomaly-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;

      .anomaly-metric {
        font-size: 14px;
        font-weight: 600;
        color: #1f2937;
      }

      .anomaly-date {
        font-size: 12px;
        color: #9ca3af;
      }
    }

    .anomaly-content {
      margin-bottom: 16px;

      .anomaly-description {
        font-size: 14px;
        color: #374151;
        margin-bottom: 12px;
      }

      .anomaly-values {
        display: flex;
        gap: 20px;

        .value-item {
          .value-label {
            font-size: 12px;
            color: #6b7280;
            display: block;
          }

          .value-actual {
            font-size: 16px;
            font-weight: 600;
            color: #374151;
          }

          .value-expected {
            font-size: 14px;
            color: #6b7280;
          }

          .value-deviation {
            font-size: 14px;
            font-weight: 600;

            &.spike { color: #ef4444; }
            &.drop { color: #3b82f6; }
          }
        }
      }
    }

    .anomaly-causes {
      margin-bottom: 16px;

      .causes-header {
        font-size: 12px;
        color: #6b7280;
        margin-bottom: 8px;
      }

      .causes-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .cause-tag {
          padding: 4px 10px;
          background-color: rgba(0, 0, 0, 0.05);
          border-radius: 4px;
          font-size: 12px;
          color: #4b5563;
        }
      }
    }

    .anomaly-root-causes {
      padding-top: 12px;
      border-top: 1px solid rgba(0, 0, 0, 0.08);

      .root-causes-header {
        font-size: 12px;
        color: #6b7280;
        margin-bottom: 8px;
      }

      .root-causes-list {
        .root-cause-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 0;
          font-size: 13px;

          .root-dimension {
            color: #6b7280;
          }

          .root-value {
            color: #374151;
            font-weight: 500;
          }

          .root-contribution {
            margin-left: auto;
            color: #3b82f6;
            font-weight: 500;
          }
        }
      }
    }
  }
}

.prediction-content,
.multi-dimension-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  .chart-area {
    width: 100%;
    height: 350px;
  }

  .prediction-table,
  .dimension-table {
    overflow-x: auto;

    table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: 12px;
        text-align: left;
        border-bottom: 1px solid #e5e7eb;
        font-size: 14px;
      }

      th {
        background-color: #f9fafb;
        font-weight: 600;
        color: #374151;
      }

      .predicted-value {
        font-weight: 600;
        color: #3b82f6;
      }

      .confidence-interval {
        color: #6b7280;
        font-size: 13px;
      }

      .up { color: #10b981; }
      .down { color: #ef4444; }
    }
  }
}

.trend-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;

  .chart-area {
    width: 100%;
    height: 400px;
  }

  .trend-summary {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    .trend-stat-item {
      padding: 20px;
      background-color: #ffffff;
      border-radius: 12px;
      border: 1px solid #e5e7eb;
      transition: all 0.2s ease;

      &:hover {
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
      }

      &.primary {
        border-color: #3b82f6;
        background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
        
        .trend-stat-value.primary {
          color: #3b82f6;
        }
      }

      &.growth {
        border-color: #10b981;
        background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
      }

      &.amount {
        border-color: #f59e0b;
        background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
        
        .trend-stat-value.amount {
          color: #f59e0b;
        }
      }

      &.info {
        border-color: #3b82f6;
        background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
      }

      &.negative {
        border-color: #ef4444;
        background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
      }

      &.positive {
        border-color: #10b981;
        background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
      }

      &.success {
        border-color: #10b981;
        background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
      }

      .trend-stat-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;
      }

      .trend-stat-label {
        font-size: 13px;
        font-weight: 600;
        color: #6b7280;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .trend-stat-badge {
        font-size: 11px;
        font-weight: 600;
        padding: 3px 8px;
        border-radius: 12px;
        
        &.positive, &.success {
          background-color: #dcfce7;
          color: #10b981;
        }
        
        &.negative {
          background-color: #fee2e2;
          color: #ef4444;
        }
        
        &.warning {
          background-color: #fef3c7;
          color: #f59e0b;
        }
        
        &.info {
          background-color: #dbeafe;
          color: #3b82f6;
        }
      }

      .trend-stat-value {
        font-size: 28px;
        font-weight: 700;
        color: #1f2937;
        line-height: 1.2;
        display: block;
        margin-bottom: 10px;

        &.positive { color: #10b981; }
        &.negative { color: #ef4444; }
        &.info { color: #3b82f6; }
        &.success { color: #10b981; }
      }

      .trend-stat-description {
        font-size: 12px;
        color: #6b7280;
        line-height: 1.6;
        background-color: #f9fafb;
        padding: 10px 12px;
        border-radius: 8px;
        margin-top: 8px;
      }
    }
  }
}

.basic-analysis-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;

  .tab-header.sub-tab {
    padding: 8px 16px;
    font-size: 13px;

    &.active {
      background-color: #eff6ff;
      color: #3b82f6;
    }
  }
}

.basic-analysis-content {
  .tab-panel {
    animation: fadeIn 0.2s ease;
  }

  .analysis-header {
    margin-bottom: 20px;

    h3 {
      margin: 0 0 4px 0;
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    .analysis-desc {
      margin: 0;
      font-size: 13px;
      color: #6b7280;
    }
  }

  .chart-wrapper {
    width: 100%;
    height: 300px;
    margin-top: 20px;
  }
}

.basic-summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.basic-stat-card {
  padding: 20px;
  background-color: #ffffff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;

  &:hover {
    border-color: #3b82f6;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
  }

  .basic-stat-header {
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f3f4f6;

    .basic-stat-title {
      font-size: 16px;
      font-weight: 700;
      color: #1f2937;
      display: block;
    }

    .basic-stat-subtitle {
      font-size: 12px;
      color: #6b7280;
      margin-top: 2px;
      display: block;
    }

    .basic-stat-label {
      font-size: 13px;
      font-weight: 600;
      color: #6b7280;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  }

  .basic-stat-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .basic-stat-item {
    .basic-stat-label {
      font-size: 12px;
      color: #9ca3af;
      display: block;
      margin-bottom: 4px;
    }

    .basic-stat-value {
      font-size: 16px;
      font-weight: 700;
      color: #1f2937;
    }
  }

  .basic-stat-value-large {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    margin-top: 8px;
    line-height: 1.2;
  }
}

.trip-analysis {
  .trip-summary-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 24px;
  }

  .trip-chart-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
    margin-bottom: 24px;
  }

  .trip-chart-container {
    background-color: #ffffff;
    padding: 20px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;

    .chart-title {
      font-size: 14px;
      font-weight: 600;
      color: #374151;
      margin-bottom: 16px;
    }

    .small-chart {
      width: 100%;
      height: 250px;
    }

    .chart-summary {
      display: flex;
      justify-content: space-around;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #f3f4f6;

      .chart-summary-item {
        text-align: center;

        .summary-label {
          font-size: 12px;
          color: #9ca3af;
          display: block;
        }

        .summary-value {
          font-size: 14px;
          font-weight: 600;
          color: #3b82f6;
          margin-top: 4px;
        }
      }
    }
  }

  .trip-insights {
    .insight-card {
      display: flex;
      gap: 16px;
      padding: 20px;
      background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
      border-radius: 8px;
      border-left: 4px solid #f59e0b;

      .insight-icon {
        font-size: 28px;
        flex-shrink: 0;
      }

      .insight-content {
        h4 {
          margin: 0 0 8px 0;
          font-size: 14px;
          font-weight: 600;
          color: #92400e;
        }

        p {
          margin: 0;
          font-size: 13px;
          color: #b45309;
          line-height: 1.5;
        }
      }
    }
  }
}

.skeleton-content {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 24px;

  .skeleton-card {
    height: 200px;
    background-color: #f2f2f2;
    border-radius: 8px;
    animation: skeleton-loading 1.5s infinite;
  }
}

@keyframes skeleton-loading {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

@media (max-width: 768px) {
  .overview-summary {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .overview-content {
    grid-template-columns: 1fr !important;
  }

  .prediction-content,
  .multi-dimension-content,
  .trend-content {
    grid-template-columns: 1fr !important;
  }

  .trend-summary {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .trip-chart-row {
    grid-template-columns: 1fr !important;
  }

  .analysis-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
<template>
  <div class="fee-composition-analysis">
    <div class="panel-header">
      <h3 class="panel-title">费用构成分析</h3>
      <p class="panel-desc">基于analysis_fee_composition表的车费、附加费、税费、小费等费用结构分析</p>
    </div>

    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div class="summary-cards">
        <div class="summary-card">
          <div class="card-indicator"></div>
          <div class="card-body">
            <div class="summary-label">总金额</div>
            <div class="summary-value">${{ formatNumber(totalAmount, 2) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator fare"></div>
          <div class="card-body">
            <div class="summary-label">基础车费占比</div>
            <div class="summary-value">{{ baseFareRatio }}%</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator tip"></div>
          <div class="card-body">
            <div class="summary-label">小费总额</div>
            <div class="summary-value">${{ formatNumber(totalTip, 2) }}</div>
          </div>
        </div>
        <div class="summary-card">
          <div class="card-indicator surcharge"></div>
          <div class="card-body">
            <div class="summary-label">附加费总额</div>
            <div class="summary-value">${{ formatNumber(totalSurcharge, 2) }}</div>
          </div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-container">
          <div ref="sunburstChartRef" class="chart-area"></div>
        </div>
        <div class="chart-container">
          <div ref="treemapChartRef" class="chart-area"></div>
        </div>
      </div>

      <div class="insight-box" v-if="insights.length > 0">
        <div class="insight-header">
          <span class="insight-icon">💡</span>
          <span class="insight-title">分析洞察</span>
        </div>
        <ul class="insight-list">
          <li v-for="(insight, idx) in insights" :key="idx">{{ insight }}</li>
        </ul>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps<{
  data: any[]
  loading: boolean
}>()

const sunburstChartRef = ref<HTMLElement | null>(null)
const treemapChartRef = ref<HTMLElement | null>(null)
let sunburstChart: echarts.ECharts | null = null
let treemapChart: echarts.ECharts | null = null

const feeInfoMap: Record<string, { name: string; description: string; color: string; category: string }> = {
  'fare': { name: '基础车费', description: '基于行驶距离和时间计算的基础费用', color: '#3b82f6', category: '核心费用' },
  'extra': { name: '附加费', description: '高峰时段、节假日等特殊时段的额外费用', color: '#f59e0b', category: '附加费用' },
  'mta_tax': { name: 'MTA税费', description: '纽约市交通局收取的税费', color: '#10b981', category: '税费' },
  'tip': { name: '小费', description: '乘客自愿支付的小费金额', color: '#8b5cf6', category: '附加费用' },
  'tolls': { name: '过路费', description: '高速公路、桥梁、隧道等过路费用', color: '#ef4444', category: '附加费用' },
  'improvement_surcharge': { name: '改善附加费', description: '用于改善出租车服务的附加费用', color: '#06b6d4', category: '税费' }
}

const processedData = computed(() => {
  return props.data.map(item => {
    const code = item.fee_code || item.code || ''
    const info = feeInfoMap[code] || { name: code, description: '', color: '#6b7280', category: '其他' }
    return {
      code,
      name: item.fee_name || info.name,
      amount: item.total_amount || item.amount || 0,
      ratio: item.percentage || 0,
      description: info.description,
      color: info.color,
      category: info.category
    }
  }).sort((a, b) => b.amount - a.amount)
})

const totalAmount = computed(() => processedData.value.reduce((sum, d) => sum + d.amount, 0))
const totalTip = computed(() => processedData.value.find(d => d.code === 'tip')?.amount || 0)
const totalSurcharge = computed(() => processedData.value.filter(d => d.code !== 'tip').reduce((sum, d) => sum + d.amount, 0))
const baseFareRatio = computed(() => {
  const base = processedData.value.find(d => d.code === 'fare')
  return base ? base.ratio.toFixed(1) : '0'
})

const insights = computed(() => {
  const result: string[] = []
  const tipData = processedData.value.find(d => d.code === 'tip')
  const fareData = processedData.value.find(d => d.code === 'fare')

  if (tipData && fareData) {
    const tipToFareRatio = fareData.amount > 0 ? (tipData.amount / fareData.amount) * 100 : 0
    if (tipToFareRatio > 20) {
      result.push(`小费与基础车费比率为${tipToFareRatio.toFixed(1)}%，乘客小费意愿较高，服务质量获得认可`)
    } else if (tipToFareRatio < 10) {
      result.push(`小费与基础车费比率仅为${tipToFareRatio.toFixed(1)}%，建议提升服务质量以提高小费收入`)
    }
  }

  const tollsData = processedData.value.find(d => d.code === 'tolls')
  if (tollsData && tollsData.ratio > 10) {
    result.push(`过路费占比${tollsData.ratio.toFixed(1)}%，长途订单比例较高，建议关注跨区出行需求`)
  }

  const extraData = processedData.value.find(d => d.code === 'extra')
  if (extraData && extraData.ratio > 5) {
    result.push(`附加费占比${extraData.ratio.toFixed(1)}%，高峰时段订单贡献较多`)
  }

  const coreFees = processedData.value.filter(d => d.category === '核心费用')
  const extraFees = processedData.value.filter(d => d.category === '附加费用')
  const taxes = processedData.value.filter(d => d.category === '税费')

  const coreRatio = coreFees.reduce((sum, d) => sum + d.amount, 0) / totalAmount.value * 100
  const extraRatio = extraFees.reduce((sum, d) => sum + d.amount, 0) / totalAmount.value * 100
  const taxRatio = taxes.reduce((sum, d) => sum + d.amount, 0) / totalAmount.value * 100

  result.push(`费用结构分布：核心费用${coreRatio.toFixed(1)}%、附加费用${extraRatio.toFixed(1)}%、税费${taxRatio.toFixed(1)}%`)

  return result
})

const formatNumber = (num: number, decimals = 0): string => {
  if (num === undefined || num === null) return '0'
  return num.toLocaleString('en-US', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })
}

const renderCharts = () => {
  if (props.data.length === 0) return
  nextTick(() => {
    renderSunburstChart()
    renderTreemapChart()
  })
}

const renderSunburstChart = () => {
  if (!sunburstChartRef.value) return
  sunburstChart?.dispose()
  sunburstChart = echarts.init(sunburstChartRef.value)

  const categories = ['核心费用', '附加费用', '税费']
  const categoryData = categories.map(cat => {
    const items = processedData.value.filter(d => d.category === cat)
    return {
      name: cat,
      children: items.map(item => ({
        name: item.name,
        value: item.amount,
        itemStyle: { color: item.color }
      }))
    }
  })

  const option: echarts.EChartsOption = {
    title: {
      text: '费用构成旭日图',
      left: 'center',
      top: 10,
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const value = params.value || 0
        const ratio = totalAmount.value > 0 ? (value / totalAmount.value * 100).toFixed(2) : '0'
        return `<div style="padding:8px"><strong>${params.name}</strong><br/>
          金额: <strong>$${value.toLocaleString()}</strong><br/>
          占比: <strong>${ratio}%</strong></div>`
      }
    },
    series: [{
      type: 'sunburst',
      data: categoryData,
      radius: [0, '90%'],
      label: {
        rotate: 'radial',
        fontSize: 11,
        fontWeight: 500,
        color: '#4b5563'
      },
      itemStyle: {
        borderRadius: 6,
        borderColor: '#ffffff',
        borderWidth: 2
      },
      levels: [
        {},
        {
          r0: '15%',
          r: '35%',
          itemStyle: {
            borderWidth: 2
          },
          label: {
            rotate: 0,
            fontSize: 12,
            fontWeight: 600
          }
        },
        {
          r0: '35%',
          r: '70%',
          label: {
            align: 'right'
          }
        },
        {
          r0: '70%',
          r: '72%',
          label: {
            position: 'outside',
            padding: 3,
            silent: false
          },
          itemStyle: {
            borderWidth: 3
          }
        }
      ],
      emphasis: {
        focus: 'ancestor'
      },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    }]
  }

  sunburstChart.setOption(option)
}

const renderTreemapChart = () => {
  if (!treemapChartRef.value) return
  treemapChart?.dispose()
  treemapChart = echarts.init(treemapChartRef.value)

  const option: echarts.EChartsOption = {
    title: {
      text: '费用构成矩形树图',
      left: 'center',
      top: 10,
      textStyle: { fontSize: 14, fontWeight: 500, color: '#374151' }
    },
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e5e7eb',
      borderWidth: 1,
      textStyle: { color: '#374151', fontSize: 12 },
      formatter: (params: any) => {
        const value = params.value || 0
        const ratio = totalAmount.value > 0 ? (value / totalAmount.value * 100).toFixed(2) : '0'
        return `<div style="padding:8px"><strong>${params.name}</strong><br/>
          金额: <strong>$${value.toLocaleString()}</strong><br/>
          占比: <strong>${ratio}%</strong></div>`
      }
    },
    series: [{
      type: 'treemap',
      data: processedData.value.map(d => ({
        name: d.name,
        value: d.amount,
        itemStyle: {
          color: d.color,
          borderColor: '#ffffff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n${c}',
          fontSize: 12,
          fontWeight: 500,
          color: '#ffffff',
          textShadowBlur: 2,
          textShadowColor: 'rgba(0,0,0,0.5)'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.15)'
          },
          label: {
            show: true,
            fontSize: 13,
            fontWeight: 600
          }
        }
      })),
      breadcrumb: { show: false },
      label: {
        show: true,
        formatter: '{b}\n${c}',
        fontSize: 12,
        fontWeight: 500,
        color: '#ffffff'
      },
      itemStyle: {
        borderColor: '#ffffff',
        borderWidth: 2,
        gapWidth: 2
      },
      animationDuration: 1500,
      animationEasing: 'cubicOut'
    } as any]
  }

  treemapChart.setOption(option)
}

const handleResize = () => {
  sunburstChart?.resize()
  treemapChart?.resize()
}

watch(() => props.data, () => renderCharts(), { deep: true })
watch(() => props.loading, (newVal) => { if (!newVal) nextTick(() => renderCharts()) })
onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (!props.loading) nextTick(() => renderCharts())
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  sunburstChart?.dispose()
  treemapChart?.dispose()
})
</script>

<style lang="scss" scoped>
.fee-composition-analysis { width: 100%; }
.panel-header { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f3f4f6; }
.panel-title { font-size: 16px; font-weight: 600; color: #1f2937; margin: 0 0 4px 0; }
.panel-desc { font-size: 14px; color: #6b7280; margin: 0; }
.loading-container { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 0; color: #6b7280; .loading-spinner { width: 40px; height: 40px; border: 3px solid #e5e7eb; border-top-color: #409eff; border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px; } }
@keyframes spin { to { transform: rotate(360deg); } }
.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { background: linear-gradient(135deg, #f0f9ff 0%, #ffffff 100%); border: 1px solid #e0f2fe; border-radius: 12px; padding: 20px; text-align: center; position: relative; overflow: hidden; }
.card-indicator { position: absolute; top: 0; left: 0; width: 4px; height: 100%; background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%); &.fare { background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%); } &.tip { background: linear-gradient(180deg, #8b5cf6 0%, #7c3aed 100%); } &.surcharge { background: linear-gradient(180deg, #f59e0b 0%, #d97706 100%); } }
.card-body { padding-left: 12px; }
.summary-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.summary-value { font-size: 24px; font-weight: 600; color: #0369a1; }
.chart-row { display: flex; gap: 24px; margin-bottom: 24px; }
.chart-container { flex: 1; background: #fafbfc; border-radius: 12px; padding: 16px; min-height: 360px; }
.chart-area { width: 100%; height: 340px; }
.insight-box { background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%); border: 1px solid #fcd34d; border-radius: 12px; padding: 20px; margin-bottom: 24px; }
.insight-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.insight-icon { font-size: 18px; }
.insight-title { font-size: 14px; font-weight: 600; color: #92400e; }
.insight-list { margin: 0; padding-left: 20px; color: #78350f; font-size: 14px; line-height: 1.8; }
</style>

<template>
  <div class="multi-dimension-container">
    <div class="multi-dimension-header">
      <h2 class="section-title">多维交叉分析</h2>
      <p class="section-desc">基于ads层数据的多维度组合深度分析</p>
    </div>

    <div class="multi-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.name"
        class="multi-tab-item"
        :class="{ active: activeTab === tab.name }"
        @click="switchTab(tab.name)"
      >
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </div>

    <div class="tab-content">
      <div v-show="activeTab === 'vendorPayment'" class="analysis-panel">
        <VendorPaymentAnalysis :data="vendorPaymentData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'airportTime'" class="analysis-panel">
        <AirportTimeAnalysis :data="airportTimeData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'boroughPayment'" class="analysis-panel">
        <BoroughPaymentAnalysis :data="boroughPaymentData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'vendorTaxiType'" class="analysis-panel">
        <VendorTaxiTypeAnalysis :data="vendorTaxiTypeData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'airportBorough'" class="analysis-panel">
        <AirportBoroughAnalysis :data="airportBoroughData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'timePayment'" class="analysis-panel">
        <TimePaymentAnalysis :data="timePaymentData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'distancePayment'" class="analysis-panel">
        <DistancePaymentAnalysis :data="distancePaymentData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'weekdayTime'" class="analysis-panel">
        <WeekdayTimeAnalysis :data="weekdayTimeData" :loading="loading" />
      </div>

      <div v-show="activeTab === 'taxiTypeFee'" class="analysis-panel">
        <TaxiTypeFeeAnalysis :data="taxiTypeFeeData" :loading="loading" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue'
import { analysisApi } from '@/api/analysis'
import VendorPaymentAnalysis from './tabs/multi/VendorPaymentAnalysis.vue'
import AirportTimeAnalysis from './tabs/multi/AirportTimeAnalysis.vue'
import BoroughPaymentAnalysis from './tabs/multi/BoroughPaymentAnalysis.vue'
import VendorTaxiTypeAnalysis from './tabs/multi/VendorTaxiTypeAnalysis.vue'
import AirportBoroughAnalysis from './tabs/multi/AirportBoroughAnalysis.vue'
import TimePaymentAnalysis from './tabs/multi/TimePaymentAnalysis.vue'
import DistancePaymentAnalysis from './tabs/multi/DistancePaymentAnalysis.vue'
import WeekdayTimeAnalysis from './tabs/multi/WeekdayTimeAnalysis.vue'
import TaxiTypeFeeAnalysis from './tabs/multi/TaxiTypeFeeAnalysis.vue'

const props = defineProps<{
  startDate: string
  endDate: string
}>()

const activeTab = ref('vendorPayment')
const loading = ref(false)

const tabs = [
  { name: 'vendorPayment', label: '供应商×支付' },
  { name: 'airportTime', label: '机场×时段' },
  { name: 'boroughPayment', label: '区域×支付' },
  { name: 'vendorTaxiType', label: '供应商×车型' },
  { name: 'airportBorough', label: '机场×区域' },
  { name: 'timePayment', label: '时段×支付' },
  { name: 'distancePayment', label: '距离×支付' },
  { name: 'weekdayTime', label: '星期×时段' },
  { name: 'taxiTypeFee', label: '车型×费用' }
]

const vendorPaymentData = ref<any[]>([])
const airportTimeData = ref<any[]>([])
const boroughPaymentData = ref<any[]>([])
const vendorTaxiTypeData = ref<any[]>([])
const airportBoroughData = ref<any[]>([])
const timePaymentData = ref<any[]>([])
const distancePaymentData = ref<any[]>([])
const weekdayTimeData = ref<any[]>([])
const taxiTypeFeeData = ref<any[]>([])

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

const fetchAllData = async () => {
  loading.value = true
  try {
    const params = { startDate: props.startDate, endDate: props.endDate }
    
    vendorPaymentData.value = await analysisApi.getVendorPaymentCross(params) || []
    await delay(300)
    
    airportTimeData.value = await analysisApi.getAirportTimeCross(params) || []
    await delay(300)
    
    boroughPaymentData.value = await analysisApi.getBoroughPaymentCross(params) || []
    await delay(300)
    
    vendorTaxiTypeData.value = await analysisApi.getVendorTaxiTypeCross(params) || []
    await delay(300)
    
    airportBoroughData.value = await analysisApi.getAirportBoroughCross(params) || []
    await delay(300)
    
    timePaymentData.value = await analysisApi.getTimePaymentCross(params) || []
    await delay(300)
    
    distancePaymentData.value = await analysisApi.getDistancePaymentCross(params) || []
    await delay(300)
    
    weekdayTimeData.value = await analysisApi.getWeekdayTimeCross(params) || []
    await delay(300)
    
    taxiTypeFeeData.value = await analysisApi.getTaxiTypeFeeCross(params) || []
  } catch (error) {
    console.error('Failed to fetch multi-dimension data:', error)
  } finally {
    loading.value = false
  }
}

const switchTab = async (tabName: string) => {
  activeTab.value = tabName
  await nextTick()
}

watch(() => [props.startDate, props.endDate], () => {
  fetchAllData()
}, { immediate: true })

onMounted(() => {
  fetchAllData()
})
</script>

<style lang="scss" scoped>
.multi-dimension-container {
  width: 100%;
}

.multi-dimension-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.section-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.multi-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 20px;
}

.multi-tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  &.active {
    border-color: #3b82f6;
    background: #3b82f6;
    color: #ffffff;

    .tab-icon,
    .tab-label {
      color: #ffffff;
    }
  }

  .tab-icon {
    font-size: 16px;
  }

  .tab-label {
    font-size: 14px;
    font-weight: 500;
    color: #374151;
  }
}

.tab-content {
  .analysis-panel {
    width: 100%;
  }
}
</style>

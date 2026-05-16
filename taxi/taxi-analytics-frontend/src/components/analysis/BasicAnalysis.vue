<template>
  <div class="basic-analysis-container">
    <div class="basic-analysis-header">
      <h2 class="section-title">基础业务分析</h2>
      <p class="section-desc">基于ads层18张业务表的单维度深度分析</p>
    </div>

    <div class="basic-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.name"
        class="basic-tab-item"
        :class="{ active: activeTab === tab.name }"
        @click="switchTab(tab.name)"
      >
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </div>

    <div class="tab-content">
      <div v-show="activeTab === 'airport'" class="analysis-panel">
        <AirportAnalysis :data="airportStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'vendor'" class="analysis-panel">
        <VendorAnalysis :data="vendorStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'payment'" class="analysis-panel">
        <PaymentAnalysis :data="paymentStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'trip'" class="analysis-panel">
        <TripFeatureAnalysis
          :passenger-data="passengerStats"
          :tip-data="tipStats"
          :distance-data="distanceStats"
          :duration-data="durationStats"
          :loading="loading"
        />
      </div>

      <div v-show="activeTab === 'hourly'" class="analysis-panel">
        <HourlyAnalysis :data="hourlyStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'weekday'" class="analysis-panel">
        <WeekdayAnalysis :data="weekdayStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'fee'" class="analysis-panel">
        <FeeCompositionAnalysis :data="feeStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'borough'" class="analysis-panel">
        <BoroughRevenueAnalysis :data="boroughStats" :loading="loading" />
      </div>

      <div v-show="activeTab === 'hotspots'" class="analysis-panel">
        <HotspotsAnalysis
          :pickup-data="pickupHotspots"
          :dropoff-data="dropoffHotspots"
          :loading="loading"
        />
      </div>

      <div v-show="activeTab === 'taxi-type'" class="analysis-panel">
        <TaxiTypeAnalysis :data="taxiTypeStats" :loading="loading" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { analysisApi } from '@/api/analysis'
import AirportAnalysis from './tabs/AirportAnalysis.vue'
import VendorAnalysis from './tabs/VendorAnalysis.vue'
import PaymentAnalysis from './tabs/PaymentAnalysis.vue'
import TripFeatureAnalysis from './tabs/TripFeatureAnalysis.vue'
import HourlyAnalysis from './tabs/HourlyAnalysis.vue'
import WeekdayAnalysis from './tabs/WeekdayAnalysis.vue'
import FeeCompositionAnalysis from './tabs/FeeCompositionAnalysis.vue'
import BoroughRevenueAnalysis from './tabs/BoroughRevenueAnalysis.vue'
import HotspotsAnalysis from './tabs/HotspotsAnalysis.vue'
import TaxiTypeAnalysis from './tabs/TaxiTypeAnalysis.vue'

const props = defineProps<{
  startDate: string
  endDate: string
}>()

const activeTab = ref('airport')
const loading = ref(false)

const tabs = [
  { name: 'airport', label: '机场运营' },
  { name: 'vendor', label: '供应商绩效' },
  { name: 'payment', label: '支付方式' },
  { name: 'trip', label: '行程特征' },
  { name: 'hourly', label: '时段分布' },
  { name: 'weekday', label: '星期分析' },
  { name: 'fee', label: '费用构成' },
  { name: 'borough', label: '区域收入' },
  { name: 'hotspots', label: '热点分析' },
  { name: 'taxi-type', label: '车型费用' }
]

const airportStats = ref<any[]>([])
const vendorStats = ref<any[]>([])
const paymentStats = ref<any[]>([])
const passengerStats = ref<any[]>([])
const tipStats = ref<any[]>([])
const distanceStats = ref<any[]>([])
const durationStats = ref<any[]>([])
const hourlyStats = ref<any[]>([])
const weekdayStats = ref<any[]>([])
const feeStats = ref<any[]>([])
const boroughStats = ref<any[]>([])
const pickupHotspots = ref<any[]>([])
const dropoffHotspots = ref<any[]>([])
const taxiTypeStats = ref<any[]>([])

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

const fetchAllData = async () => {
  loading.value = true
  try {
    const params = { startDate: props.startDate, endDate: props.endDate }
    
    airportStats.value = await analysisApi.getAirportStatistics(params) || []
    await delay(200)
    
    vendorStats.value = await analysisApi.getVendorComparison(params) || []
    await delay(200)
    
    paymentStats.value = await analysisApi.getPaymentDistribution(params) || []
    await delay(200)
    
    passengerStats.value = await analysisApi.getPassengerDistribution(params) || []
    await delay(200)
    
    tipStats.value = await analysisApi.getTipDistribution(params) || []
    await delay(200)
    
    distanceStats.value = await analysisApi.getDistanceDistribution(params) || []
    await delay(200)
    
    durationStats.value = await analysisApi.getDurationDistribution(params) || []
    await delay(200)
    
    hourlyStats.value = await analysisApi.getHourlyDistribution(params) || []
    await delay(200)
    
    weekdayStats.value = await analysisApi.getWeekdayAnalysis(params) || []
    await delay(200)
    
    feeStats.value = await analysisApi.getFeeComposition(params) || []
    await delay(200)
    
    boroughStats.value = await analysisApi.getBoroughRevenue(params) || []
    await delay(200)
    
    taxiTypeStats.value = await analysisApi.getTaxiTypeFee(params) || []
    await delay(200)
    
    pickupHotspots.value = await analysisApi.getPickupHotspots({ ...params, limit: 10 }) || []
    await delay(200)
    
    dropoffHotspots.value = await analysisApi.getDropoffHotspots({ ...params, limit: 10 }) || []
  } catch (error) {
    console.error('Failed to fetch analysis data:', error)
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
  margin: 0 0 4px 0;
}

.section-desc {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.basic-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
  padding: 8px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-wrap: wrap;
}

.basic-tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  .tab-icon {
    font-size: 16px;
  }

  .tab-label {
    white-space: nowrap;
  }

  &:hover {
    background: #f3f4f6;
  }

  &.active {
    background: linear-gradient(135deg, #409eff 0%, #3b82f6 100%);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
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
</style>

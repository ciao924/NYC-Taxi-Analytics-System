<template>
  <div class="date-range-picker">
    <el-date-picker
      v-model="dateRange"
      type="daterange"
      range-separator="至"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      format="YYYY-MM-DD"
      value-format="YYYY-MM-DD"
      :picker-options="pickerOptions"
      @change="handleDateChange"
      class="date-picker"
    />
    <div class="quick-options">
      <el-button 
        v-for="option in quickOptions" 
        :key="option.key"
        :type="activeQuickOption === option.key ? 'primary' : 'default'"
        size="small"
        @click="selectQuickOption(option)"
      >
        {{ option.label }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue?: [string, string]
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: [string, string]): void
  (e: 'change', value: [string, string]): void
}>()

const dateRange = ref<[string, string]>(props.modelValue || [getDateString(7), getDateString(0)])
const activeQuickOption = ref<string>('7d')

const quickOptions = [
  { key: '1d', label: '今日', days: 1 },
  { key: '7d', label: '近7天', days: 7 },
  { key: '30d', label: '近30天', days: 30 },
  { key: '90d', label: '近90天', days: 90 },
  { key: 'mtd', label: '本月', type: 'month' },
  { key: 'qtd', label: '本季', type: 'quarter' },
  { key: 'ytd', label: '本年', type: 'year' },
  { key: 'custom', label: '自定义' }
]

const pickerOptions = {
  disabledDate(time: Date) {
    return time.getTime() > Date.now()
  },
  shortcuts: [
    { text: '今日', value: () => [new Date(), new Date()] },
    { text: '近7天', value: () => [new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), new Date()] },
    { text: '近30天', value: () => [new Date(Date.now() - 30 * 24 * 60 * 60 * 1000), new Date()] },
    { text: '本月', value: () => [new Date(new Date().getFullYear(), new Date().getMonth(), 1), new Date()] },
    { text: '本季', value: () => {
        const now = new Date()
        const quarterStartMonth = Math.floor(now.getMonth() / 3) * 3
        return [new Date(now.getFullYear(), quarterStartMonth, 1), now]
      }
    },
    { text: '本年', value: () => [new Date(new Date().getFullYear(), 0, 1), new Date()] }
  ]
}

function getDateString(days: number): string {
  const date = new Date()
  date.setDate(date.getDate() - days)
  return date.toISOString().split('T')[0]
}

function handleDateChange(value: [string, string] | null) {
  if (value) {
    dateRange.value = value
    activeQuickOption.value = 'custom'
    emit('update:modelValue', value)
    emit('change', value)
  }
}

function selectQuickOption(option: any) {
  if (option.type === 'month') {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth(), 1)
    const end = new Date()
    dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  } else if (option.type === 'quarter') {
    const now = new Date()
    const quarterStartMonth = Math.floor(now.getMonth() / 3) * 3
    const start = new Date(now.getFullYear(), quarterStartMonth, 1)
    const end = new Date()
    dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  } else if (option.type === 'year') {
    const now = new Date()
    const start = new Date(now.getFullYear(), 0, 1)
    const end = new Date()
    dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  } else if (option.days) {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - option.days + 1)
    dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  }
  activeQuickOption.value = option.key
  emit('update:modelValue', dateRange.value)
  emit('change', dateRange.value)
}

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    dateRange.value = newValue
  }
}, { deep: true })
</script>

<style scoped>
.date-range-picker {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.date-picker {
  min-width: 280px;
}

.quick-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .date-range-picker {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .quick-options {
    width: 100%;
  }
  
  .quick-options .el-button {
    flex: 1;
    min-width: 80px;
  }
}
</style>
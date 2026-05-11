<template>
  <el-tag :type="statusType" size="small" effect="plain">
    <span>{{ iconText }}</span>
    <span>{{ statusText }}</span>
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export type DataStatus = 'normal' | 'updating' | 'delay' | 'unknown'

const props = defineProps<{
  status: DataStatus
  lastUpdateTime?: string
}>()

const statusType = computed(() => {
  switch (props.status) {
    case 'normal': return 'success'
    case 'updating': return 'warning'
    case 'delay': return 'danger'
    default: return 'info'
  }
})

const iconText = computed(() => {
  switch (props.status) {
    case 'normal': return '✓'
    case 'updating': return '○'
    case 'delay': return '⚠'
    default: return '?'
  }
})

const statusText = computed(() => {
  switch (props.status) {
    case 'normal': return `数据已更新${props.lastUpdateTime ? ` (${props.lastUpdateTime})` : ''}`
    case 'updating': return '数据更新中，仅供参考'
    case 'delay': return '数据延迟，请耐心等待'
    default: return '数据状态未知'
  }
})
</script>
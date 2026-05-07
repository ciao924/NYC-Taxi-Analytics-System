<template>
  <div class="empty-state" :class="type">
    <div class="empty-icon">
      <component :is="iconMap[type]" />
    </div>
    <div class="empty-title">{{ title }}</div>
    <div class="empty-description">{{ description }}</div>
    <div v-if="showAction" class="empty-action">
      <el-button type="primary" @click="onAction">{{ actionText }}</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  type?: 'no-data' | 'no-filter' | 'error' | 'network'
  title?: string
  description?: string
  showAction?: boolean
  actionText?: string
}>(), {
  type: 'no-data',
  showAction: false,
  actionText: '刷新'
})

const emit = defineEmits<{
  (e: 'action'): void
}>()

const iconMap = {
  'no-data': '📊',
  'no-filter': '🔍',
  'error': '⚠️',
  'network': '🌐'
}

const titleMap = {
  'no-data': '暂无数据',
  'no-filter': '请选择筛选条件',
  'error': '数据加载失败',
  'network': '网络连接异常'
}

const descriptionMap = {
  'no-data': '当前日期范围内没有数据，请调整筛选条件',
  'no-filter': '请选择日期范围后查询',
  'error': '数据加载失败，请稍后重试',
  'network': '请检查网络连接后刷新'
}

const title = computed(() => props.title || titleMap[props.type])
const description = computed(() => props.description || descriptionMap[props.type])

const onAction = () => {
  emit('action')
}
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #909399;
  margin-bottom: 8px;
}

.empty-description {
  font-size: 14px;
  color: #c0c4cc;
  margin-bottom: 20px;
}
</style>
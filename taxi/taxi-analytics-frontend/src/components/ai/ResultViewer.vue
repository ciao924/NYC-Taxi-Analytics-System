<template>
  <div class="result-viewer">
    <div class="result-header">
      <div class="result-title">{{ title }}</div>
      <div class="view-mode-selector">
        <el-radio-group v-model="viewMode" @change="handleViewModeChange">
          <el-radio value="chart">图表</el-radio>
          <el-radio value="table">表格</el-radio>
          <el-radio value="text">文本</el-radio>
        </el-radio-group>
      </div>
    </div>
    
    <div class="result-content">
      <div v-if="viewMode === 'chart'" class="chart-view">
        <!-- 图表内容 -->
        <slot name="chart"></slot>
      </div>
      <div v-else-if="viewMode === 'table'" class="table-view">
        <!-- 表格内容 -->
        <el-table :data="tableData" style="width: 100%">
          <el-table-column 
            v-for="column in tableColumns" 
            :key="column.prop"
            :prop="column.prop"
            :label="column.label"
            :width="column.width"
            :formatter="column.formatter"
          />
        </el-table>
      </div>
      <div v-else-if="viewMode === 'text'" class="text-view">
        <!-- 文本内容 -->
        <div class="text-content">{{ textContent }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  title: string
  tableData?: any[]
  tableColumns?: Array<{
    prop: string
    label: string
    width?: string
    formatter?: (row: any, column: any, cellValue: any, index: number) => string
  }>
  textContent?: string
  defaultViewMode?: 'chart' | 'table' | 'text'
}>()

const emit = defineEmits<{
  (e: 'viewModeChange', mode: 'chart' | 'table' | 'text'): void
}>()

const viewMode = ref< 'chart' | 'table' | 'text'>(props.defaultViewMode || 'chart')

const handleViewModeChange = (mode: 'chart' | 'table' | 'text') => {
  viewMode.value = mode
  emit('viewModeChange', mode)
}
</script>

<style scoped>
.result-viewer {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-top: 12px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.view-mode-selector {
  display: flex;
  gap: 16px;
}

.result-content {
  width: 100%;
}

.chart-view,
.table-view,
.text-view {
  width: 100%;
  min-height: 300px;
}

.table-view {
  overflow-x: auto;
}

.text-view {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .result-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
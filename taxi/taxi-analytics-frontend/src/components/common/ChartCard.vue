<template>
  <div class="chart-card">
    <div class="chart-card-header">
      <div class="chart-card-title">{{ title }}</div>
      <div class="chart-card-actions">
        <slot name="actions">
          <el-button 
            v-if="showRefresh" 
            size="small" 
            @click="$emit('refresh')"
            :loading="loading"
          >
            刷新
          </el-button>
          <el-button 
            v-if="showExport" 
            size="small" 
            @click="$emit('export')"
          >
            导出
          </el-button>
        </slot>
      </div>
    </div>
    
    <div class="chart-card-body">
      <template v-if="loading">
        <div class="chart-card-loading">
          <el-skeleton :rows="3" animated />
        </div>
      </template>
      <template v-else>
        <slot></slot>
      </template>
    </div>
    
    <div class="chart-card-footer" v-if="showFooter">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  loading?: boolean
  showRefresh?: boolean
  showExport?: boolean
  showFooter?: boolean
}>(), {
  loading: false,
  showRefresh: true,
  showExport: true,
  showFooter: false
})

defineEmits<{
  (e: 'refresh'): void
  (e: 'export'): void
}>()
</script>

<style scoped>
.chart-card {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s;
}

.chart-card:hover {
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.chart-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #ebeef5;
  background-color: #f5f7fa;
}

.chart-card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.chart-card-actions {
  display: flex;
  gap: 8px;
}

.chart-card-body {
  padding: 20px;
  min-height: 300px;
}

.chart-card-loading {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-card-footer {
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
  background-color: #f9fafc;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .chart-card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .chart-card-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .chart-card-body {
    padding: 16px;
    min-height: 200px;
  }
}
</style>
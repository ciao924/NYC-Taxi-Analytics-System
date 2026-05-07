<template>
  <div id="app">
    <el-container>
      <el-header height="60px" class="app-header">
        <div class="header-left">
          <h1 class="app-title">出租车数据分析系统</h1>
        </div>
        <div class="header-right">
          <el-menu :default-active="activeIndex" mode="horizontal" background-color="#fff" text-color="#333" active-text-color="#409EFF" router>
            <el-menu-item index="/dashboard">
              <template #title>
                <span>可视化模块</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/map/heatmap">
              <template #title>
                <span>热力地图</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/analysis">
              <template #title>
                <span>数据分析</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/quality">
              <template #title>
                <span>数据检测模块</span>
              </template>
            </el-menu-item>
            <el-menu-item index="/ai">
              <template #title>
                <span>AI智能体模块</span>
              </template>
            </el-menu-item>
          </el-menu>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeIndex = computed(() => route.path)
</script>

<style scoped>
.app-header {
  border-bottom: 1px solid #e4e7ed;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.app-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
  color: #333;
}

.header-right {
  flex: 1;
  max-width: 600px;
}

.app-main {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}
</style>

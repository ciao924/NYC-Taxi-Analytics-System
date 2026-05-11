<template>
  <div id="app" class="app-container">
    <el-container class="app-layout">
      <el-header class="app-header">
        <div class="header-brand">
          <span class="brand-title">出租车数据分析系统</span>
        </div>
        <div class="header-nav">
          <el-menu
            :default-active="activeRoute"
            mode="horizontal"
            class="nav-menu"
            router
          >
            <el-menu-item index="/dashboard">
              <span>数据看板</span>
            </el-menu-item>
            <el-menu-item index="/realtime">
              <span>实时监控</span>
            </el-menu-item>
            <el-menu-item index="/quality">
              <span>质量检测</span>
            </el-menu-item>
            <el-menu-item index="/analysis">
              <span>深度分析</span>
            </el-menu-item>
            <el-menu-item index="/map">
              <span>热力地图</span>
            </el-menu-item>
            <el-menu-item index="/ai">
              <span>AI智能助手</span>
            </el-menu-item>
          </el-menu>
        </div>
        <div class="header-actions">
          <el-button
            type="text"
            class="theme-toggle"
            @click="toggleDarkMode"
          >
            {{ isDarkMode ? '☀' : '☽' }}
          </el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isDarkMode = ref(false)

const activeRoute = computed(() => route.path)

const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value
  document.documentElement.classList.toggle('dark', isDarkMode.value)
  localStorage.setItem('darkMode', String(isDarkMode.value))
}

onMounted(() => {
  const saved = localStorage.getItem('darkMode')
  if (saved !== null) {
    isDarkMode.value = saved === 'true'
    document.documentElement.classList.toggle('dark', isDarkMode.value)
  }
})

watch(isDarkMode, (val) => {
  document.documentElement.classList.toggle('dark', val)
})
</script>

<style lang="scss">
.app-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.app-layout {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.header-nav {
  flex: 1;
  max-width: 600px;
}

.nav-menu {
  border: none;
  background: transparent;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  padding: 8px;
  font-size: 20px;
  color: #6b7280;
}

.app-main {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.dark {
  .app-container {
    background-color: #1f2937;
  }

  .app-header {
    background-color: #111827;
    border-bottom-color: #374151;
  }

  .brand-title {
    color: #f9fafb;
  }

  .theme-toggle {
    color: #d1d5db;
  }

  .app-main {
    background-color: #1f2937;
  }
}
</style>

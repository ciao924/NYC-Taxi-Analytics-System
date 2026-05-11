<template>
  <div id="app" class="app-container" @contextmenu.prevent="showContextMenu">
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

    <!-- 自定义右键菜单 -->
    <Teleport to="body">
      <el-menu
        v-if="contextMenuVisible"
        :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
        class="context-menu"
        mode="vertical"
      >
        <el-menu-item @click="refreshPage">
          <span>刷新页面</span>
        </el-menu-item>
      </el-menu>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)

const activeRoute = computed(() => route.path)

const showContextMenu = (event: MouseEvent) => {
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuVisible.value = true
}

const hideContextMenu = () => {
  contextMenuVisible.value = false
}

const refreshPage = () => {
  hideContextMenu()
  const currentPath = route.path
  router.push({ path: currentPath, query: { t: Date.now().toString() } })
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.context-menu')) {
    hideContextMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
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

.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #e5e7eb;
  padding: 4px 0;

  :deep(.el-menu-item) {
    padding: 8px 16px;
    font-size: 13px;
    color: #374151;
    border-radius: 4px;
    margin: 0 4px;

    &:hover {
      background-color: #f3f4f6;
      color: #1f2937;
    }
  }
}
</style>

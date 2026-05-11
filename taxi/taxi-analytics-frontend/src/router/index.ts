import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/Index.vue'),
    meta: { title: '数据看板' }
  },
  {
    path: '/realtime',
    name: 'Realtime',
    component: () => import('@/views/realtime/Index.vue'),
    meta: { title: '实时监控' }
  },
  {
    path: '/map',
    name: 'Map',
    component: () => import('@/views/map/HeatmapView.vue'),
    meta: { title: '热力地图' }
  },
  {
    path: '/analysis',
    name: 'Analysis',
    component: () => import('@/views/analysis/Index.vue'),
    meta: { title: '深度分析' }
  },
  {
    path: '/quality',
    name: 'Quality',
    component: () => import('@/views/quality/Index.vue'),
    meta: { title: '质量检测' }
  },
  {
    path: '/ai',
    name: 'AI',
    component: () => import('@/views/ai/Index.vue'),
    meta: { title: 'AI智能助手' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404 - 页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, _from, next) => {
  const title = to.meta.title ? `${to.meta.title} - 出租车数据分析系统` : '出租车数据分析系统'
  document.title = title
  next()
})

export default router

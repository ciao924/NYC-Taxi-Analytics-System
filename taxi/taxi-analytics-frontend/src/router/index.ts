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
    meta: { title: '仪表盘' }
  },
  {
    path: '/map/heatmap',
    name: 'Heatmap',
    component: () => import('@/views/map/Heatmap.vue'),
    meta: { title: '热力地图' }
  },
  {
    path: '/heatmap',
    name: 'HeatmapView',
    component: () => import('@/views/map/HeatmapView.vue'),
    meta: { title: '热力地图' }
  },
  {
    path: '/analysis',
    name: 'Analysis',
    component: () => import('@/views/analysis/Index.vue'),
    meta: { title: '数据分析' }
  },
  {
    path: '/quality',
    name: 'Quality',
    component: () => import('@/views/quality/Index.vue'),
    meta: { title: '数据质量检测' }
  },
  {
    path: '/ai',
    name: 'AI',
    component: () => import('@/views/ai/Index.vue'),
    meta: { title: '智能查询' }
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